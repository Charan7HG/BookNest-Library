package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.pentagon.LibraryManagement.Repository.BookCopyRepository;
import com.pentagon.LibraryManagement.Repository.BookRepository;
import com.pentagon.LibraryManagement.Repository.BorrowRequestRepository;
import com.pentagon.LibraryManagement.Repository.LoanRepository;
import com.pentagon.LibraryManagement.Repository.UserRepository;
import com.pentagon.LibraryManagement.Service.BorrowRequestService;
import com.pentagon.LibraryManagement.entity.Book;
import com.pentagon.LibraryManagement.entity.BookCopy;
import com.pentagon.LibraryManagement.entity.BookCopyStatus;
import com.pentagon.LibraryManagement.entity.BorrowRequest;
import com.pentagon.LibraryManagement.entity.BorrowRequestStatus;
import com.pentagon.LibraryManagement.entity.Loan;
import com.pentagon.LibraryManagement.entity.LoanStatus;
import com.pentagon.LibraryManagement.entity.Profile;
import com.pentagon.LibraryManagement.entity.Role;
import com.pentagon.LibraryManagement.entity.Users;
import com.pentagon.LibraryManagement.exception.BorrowingException;
import com.pentagon.LibraryManagement.security.JwtUtil;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:libraryborrow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "app.borrowing.loan-period-days=14",
        "app.borrowing.max-active-loans=3",
        "app.borrowing.timezone=Asia/Kolkata"
})
@AutoConfigureMockMvc
class BorrowingWorkflowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    @Autowired
    private BorrowRequestService borrowRequestService;

    @Autowired
    private JwtUtil jwtUtil;

    private Users member;
    private Users librarian;
    private Users admin;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        borrowRequestRepository.deleteAll();
        loanRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        String suffix = UUID.randomUUID().toString();
        member = account("member-" + suffix + "@example.test", Role.MEMBER);
        librarian = account("librarian-" + suffix + "@example.test", Role.LIBRARIAN);
        admin = account("admin-" + suffix + "@example.test", Role.ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestApproveReturnFlowAllocatesExactlyOneCopyAndKeepsHistory() throws Exception {
        Book book = book("Clean Architecture");
        BookCopy copy = copy(book, "BC-1", BookCopyStatus.AVAILABLE);

        MvcResult created = mockMvc.perform(post("/borrow-requests")
                .header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":" + book.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.memberId").value(member.getId()))
                .andReturn();

        int requestId = requestId(created);

        mockMvc.perform(post("/borrow-requests/{id}/approve", requestId)
                .header("Authorization", bearer(librarian)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewedById").value(librarian.getId()))
                .andExpect(jsonPath("$.data.loanId").isNumber());

        BorrowRequest approved = borrowRequestRepository.findById(requestId).orElseThrow();
        assertEquals(BorrowRequestStatus.APPROVED, approved.getStatus());
        assertNotNull(approved.getLoan());
        assertEquals(librarian.getId(), approved.getReviewedBy().getId());
        assertEquals(BookCopyStatus.ON_LOAN, bookCopyRepository.findById(copy.getId()).orElseThrow().getStatus());

        Loan loan = approved.getLoan();
        assertEquals(LoanStatus.ACTIVE, loan.getStatus());
        assertEquals(member.getId(), loan.getMember().getId());
        assertEquals(copy.getId(), loan.getCopy().getId());
        assertNotNull(loan.getIssueDate());
        assertNotNull(loan.getDueDate());
        assertEquals(14, Duration.between(loan.getIssueDate(), loan.getDueDate()).toDays());

        mockMvc.perform(get("/loans/active").header("Authorization", bearer(librarian)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].loanId").value(loan.getId()))
                .andExpect(jsonPath("$.data[0].legacy").value(false));

        mockMvc.perform(post("/loans/{id}/return", loan.getId())
                .header("Authorization", bearer(librarian)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETURNED"));

        Loan returned = loanRepository.findById(loan.getId()).orElseThrow();
        assertEquals(LoanStatus.RETURNED, returned.getStatus());
        assertNotNull(returned.getReturnedAt());
        assertEquals(BookCopyStatus.AVAILABLE, bookCopyRepository.findById(copy.getId()).orElseThrow().getStatus());

        // The returned request remains as history for the member.
        mockMvc.perform(get("/borrow-requests/my").header("Authorization", bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.data[0].returnDate").isNotEmpty());

        // Returning the same loan twice is rejected.
        mockMvc.perform(post("/loans/{id}/return", loan.getId())
                .header("Authorization", bearer(librarian)))
                .andExpect(status().isConflict());
        assertEquals(1, bookCopyRepository.count());
    }

    @Test
    void duplicatePendingRequestIsRejectedButHistoryAllowsANewRequestAfterRejection() throws Exception {
        Book book = book("Refactoring");
        copy(book, "BC-2", BookCopyStatus.AVAILABLE);

        int firstId = createRequest(member, book, 201);

        mockMvc.perform(post("/borrow-requests")
                .header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":" + book.getId() + "}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/borrow-requests/{id}/reject", firstId)
                .header("Authorization", bearer(librarian)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        assertEquals(BorrowRequestStatus.REJECTED,
                borrowRequestRepository.findById(firstId).orElseThrow().getStatus());
        assertEquals(1, borrowRequestRepository.count(), "Rejected request remains as history");

        int secondId = createRequest(member, book, 201);
        assertTrue(secondId != firstId);
        assertEquals(2, borrowRequestRepository.count());
    }

    @Test
    void approvalWithoutAvailableCopyLeavesRequestPending() throws Exception {
        Book book = book("No Copies");

        int requestId = createRequest(member, book, 201);

        mockMvc.perform(post("/borrow-requests/{id}/approve", requestId)
                .header("Authorization", bearer(librarian)))
                .andExpect(status().isConflict());

        BorrowRequest request = borrowRequestRepository.findById(requestId).orElseThrow();
        assertEquals(BorrowRequestStatus.PENDING, request.getStatus());
        assertEquals(0, loanRepository.count());
    }

    @Test
    void memberWithThreeActiveLoansCannotRequestAnotherBook() throws Exception {
        for (int i = 0; i < 3; i++) {
            Book borrowed = book("Borrowed " + i);
            BookCopy borrowedCopy = copy(borrowed, "BORROWED-" + i, BookCopyStatus.ON_LOAN);
            Loan loan = new Loan();
            loan.setCopy(borrowedCopy);
            loan.setMember(member);
            loan.setIssueDate(Instant.now());
            loan.setDueDate(Instant.now().plusSeconds(14L * 24 * 60 * 60));
            loan.setStatus(LoanStatus.ACTIVE);
            loanRepository.save(loan);
        }

        Book target = book("Target");
        copy(target, "TARGET-1", BookCopyStatus.AVAILABLE);

        mockMvc.perform(post("/borrow-requests")
                .header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":" + target.getId() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You have reached the maximum of 3 active loans"));
    }

    @Test
    void roleRestrictionsAreEnforcedAtTheHttpBoundary() throws Exception {
        Book book = book("Secure");
        copy(book, "SECURE-1", BookCopyStatus.AVAILABLE);
        int requestId = createRequest(member, book, 201);

        mockMvc.perform(post("/borrow-requests")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":" + book.getId() + "}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/borrow-requests/pending").header("Authorization", bearer(member)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/borrow-requests/{id}/approve", requestId)
                .header("Authorization", bearer(member)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/loans/active").header("Authorization", bearer(member)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/loans/{id}/return", 999)
                .header("Authorization", bearer(member)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/borrow-requests/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminAndLibrarianCanReviewPendingRequests() throws Exception {
        Book book = book("Reviewed");
        copy(book, "REVIEWED-1", BookCopyStatus.AVAILABLE);
        int requestId = createRequest(member, book, 201);

        mockMvc.perform(get("/borrow-requests/pending").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].requestId").value(requestId));

        mockMvc.perform(post("/borrow-requests/{id}/reject", requestId)
                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void twoConcurrentApprovalsCannotAllocateTheSameCopy() throws Exception {
        Users otherMember = account("other-" + UUID.randomUUID() + "@example.test", Role.MEMBER);
        Book book = book("Contended");
        copy(book, "CONTENDED-1", BookCopyStatus.AVAILABLE);

        int firstId = createRequest(member, book, 201);
        int secondId = createRequest(otherMember, book, 201);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> first = approvalTask(firstId, start);
            Callable<Boolean> second = approvalTask(secondId, start);
            Future<Boolean> firstResult = executor.submit(first);
            Future<Boolean> secondResult = executor.submit(second);

            start.countDown();

            boolean firstSucceeded = firstResult.get();
            boolean secondSucceeded = secondResult.get();

            assertTrue(firstSucceeded ^ secondSucceeded,
                    "Exactly one concurrent approval must succeed");
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, loanRepository.count());
        assertEquals(1, loanRepository.findByStatusOrderByIdAsc(LoanStatus.ACTIVE).size());
        assertEquals(BookCopyStatus.ON_LOAN, bookCopyRepository.findAll().get(0).getStatus());

        long approved = borrowRequestRepository.findAll().stream()
                .filter(request -> request.getStatus() == BorrowRequestStatus.APPROVED)
                .count();
        long pending = borrowRequestRepository.findAll().stream()
                .filter(request -> request.getStatus() == BorrowRequestStatus.PENDING)
                .count();
        assertEquals(1, approved);
        assertEquals(1, pending);
        assertNotNull(borrowRequestRepository.findByStatusOrderByRequestedAtAsc(BorrowRequestStatus.PENDING)
                .get(0));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Callable<Boolean> approvalTask(int requestId, CountDownLatch start) {
        return () -> {
            start.await();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            librarian.getProfile().getEmail(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))));
            try {
                borrowRequestService.approveRequest(requestId);
                return true;
            } catch (BorrowingException exception) {
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private int createRequest(Users owner, Book book, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/borrow-requests")
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":" + book.getId() + "}"))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return requestId(result);
    }

    private int requestId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"requestId\":(\\d+)")
                .matcher(body);
        assertTrue(matcher.find(), "Response must contain a requestId: " + body);
        return Integer.parseInt(matcher.group(1));
    }

    private Users account(String email, Role role) {
        Users user = new Users();
        user.setName(email);
        user.setRole(role);
        user.setPassword("test-password");
        Profile profile = new Profile();
        profile.setEmail(email);
        profile.setPhone(1000L);
        user.setProfile(profile);
        return userRepository.save(user);
    }

    private Book book(String name) {
        Book book = new Book();
        book.setName(name);
        book.setPrice(10.0);
        return bookRepository.save(book);
    }

    private BookCopy copy(Book book, String code, BookCopyStatus status) {
        return bookCopyRepository.save(new BookCopy(book, code, status));
    }

    private String bearer(Users user) {
        return "Bearer " + jwtUtil.generateToken(user.getProfile().getEmail(), user.getRole().name());
    }
}
