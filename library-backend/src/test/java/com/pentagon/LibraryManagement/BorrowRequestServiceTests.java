package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.pentagon.LibraryManagement.DAO.BookCopyDAO;
import com.pentagon.LibraryManagement.DAO.BookDAO;
import com.pentagon.LibraryManagement.DAO.BorrowRequestDAO;
import com.pentagon.LibraryManagement.DAO.LoanDAO;
import com.pentagon.LibraryManagement.DAO.UsersDAO;
import com.pentagon.LibraryManagement.Service.BorrowRequestService;
import com.pentagon.LibraryManagement.config.AppConfig;
import com.pentagon.LibraryManagement.dto.BorrowRequestCreateRequest;
import com.pentagon.LibraryManagement.dto.BorrowRequestResponse;
import com.pentagon.LibraryManagement.dto.LoanResponse;
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

@ExtendWith(MockitoExtension.class)
class BorrowRequestServiceTests {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-12T06:30:00Z");
    private static final int MAX_LOANS = 3;
    private static final int LOAN_DAYS = 14;

    @Mock
    private BorrowRequestDAO borrowRequestDAO;

    @Mock
    private BookCopyDAO bookCopyDAO;

    @Mock
    private LoanDAO loanDAO;

    @Mock
    private BookDAO bookDAO;

    @Mock
    private UsersDAO usersDAO;

    @Mock
    private AppConfig appConfig;

    @Mock
    private Clock clock;

    @InjectMocks
    private BorrowRequestService service;

    private Users member;
    private Users librarian;
    private Book book;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        lenient().when(clock.instant()).thenReturn(FIXED_NOW);
        lenient().when(appConfig.getMaxActiveLoans()).thenReturn(MAX_LOANS);
        lenient().when(appConfig.getLoanPeriodDays()).thenReturn(LOAN_DAYS);

        member = user(7, "Member", "member@example.test", Role.MEMBER);
        librarian = user(9, "Librarian", "librarian@example.test", Role.LIBRARIAN);
        book = book(3, "Domain-Driven Design");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // Request creation rules
    // ------------------------------------------------------------------

    @Test
    void memberCanCreateRequestAndIdentityComesFromSecurityContext() {
        authenticate(member);
        when(bookDAO.getById(3)).thenReturn(book);
        when(borrowRequestDAO.saveAndFlush(any(BorrowRequest.class))).thenAnswer(invocation -> {
            BorrowRequest saved = invocation.getArgument(0);
            saved.setId(42);
            return saved;
        });

        BorrowRequestResponse response = service.createRequest(requestFor(3));

        assertEquals(42, response.getRequestId());
        assertEquals(3, response.getBookId());
        assertEquals(7, response.getMemberId());
        assertEquals("member@example.test", response.getMemberEmail());
        assertEquals("PENDING", response.getStatus());
        assertEquals(FIXED_NOW, response.getRequestedAt());
    }

    @Test
    void onlyMembersCanCreateRequests() {
        authenticate(librarian);

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.createRequest(requestFor(3)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(bookDAO, never()).getById(3);
        verify(borrowRequestDAO, never()).saveAndFlush(any(BorrowRequest.class));
    }

    @Test
    void unauthenticatedUserCannotCreateRequest() {
        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.createRequest(requestFor(3)));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(borrowRequestDAO, never()).saveAndFlush(any(BorrowRequest.class));
    }

    @Test
    void duplicatePendingRequestIsRejected() {
        authenticate(member);
        when(bookDAO.getById(3)).thenReturn(book);
        when(borrowRequestDAO.existsPending(7, 3)).thenReturn(true);

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.createRequest(requestFor(3)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("You already have a pending request for this book", exception.getMessage());
        verify(borrowRequestDAO, never()).saveAndFlush(any(BorrowRequest.class));
    }

    @Test
    void memberCannotRequestBookAlreadyOnActiveLoan() {
        authenticate(member);
        when(bookDAO.getById(3)).thenReturn(book);
        when(loanDAO.existsActiveForMemberAndBook(7, 3)).thenReturn(true);

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.createRequest(requestFor(3)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("You already have this book on loan", exception.getMessage());
    }

    @Test
    void memberWithThreeActiveLoansCannotCreateRequest() {
        authenticate(member);
        when(bookDAO.getById(3)).thenReturn(book);
        when(loanDAO.countActiveByMember(7)).thenReturn((long) MAX_LOANS);

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.createRequest(requestFor(3)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("You have reached the maximum of 3 active loans", exception.getMessage());
    }

    @Test
    void requestForUnknownBookIsRejected() {
        authenticate(member);
        when(bookDAO.getById(3)).thenReturn(null);

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.createRequest(requestFor(3)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void requestWithoutBookIdIsRejected() {
        authenticate(member);

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.createRequest(requestFor(null)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void duplicateInsertRacingTheUniqueIndexIsReportedAsConflict() {
        authenticate(member);
        when(bookDAO.getById(3)).thenReturn(book);
        when(borrowRequestDAO.saveAndFlush(any(BorrowRequest.class)))
                .thenThrow(new DataIntegrityViolationException("uq_borrow_request_pending_member_book"));

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.createRequest(requestFor(3)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("You already have a pending request for this book", exception.getMessage());
    }

    // ------------------------------------------------------------------
    // Approval
    // ------------------------------------------------------------------

    @Test
    void approvalIssuesOneCopyAndSetsDatesFromInjectedClock() {
        authenticate(librarian);
        BorrowRequest request = pendingRequest(11, member, book);
        BookCopy copy = copy(21, book, "BC-21");
        when(borrowRequestDAO.lockById(11)).thenReturn(Optional.of(request));
        when(loanDAO.countActiveByMember(7)).thenReturn(0L);
        when(bookCopyDAO.lockFirstAvailableForBook(3)).thenReturn(Optional.of(copy));
        when(loanDAO.saveAndFlush(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            loan.setId(31);
            return loan;
        });

        BorrowRequestResponse response = service.approveRequest(11);

        assertEquals("APPROVED", response.getStatus());
        assertEquals(31, response.getLoanId());
        assertEquals(FIXED_NOW, response.getIssueDate());
        assertEquals(FIXED_NOW.plusSeconds(14L * 24 * 60 * 60), response.getDueDate());
        assertEquals(9, response.getReviewedById());
        assertEquals(FIXED_NOW, response.getReviewedAt());

        assertEquals(BorrowRequestStatus.APPROVED, request.getStatus());
        assertEquals(9, request.getReviewedBy().getId());
        assertEquals(31, request.getLoan().getId());
        assertEquals(LoanStatus.ACTIVE, request.getLoan().getStatus());
        assertEquals(FIXED_NOW, request.getLoan().getIssueDate());
        assertEquals(FIXED_NOW.plusSeconds(14L * 24 * 60 * 60), request.getLoan().getDueDate());
        assertEquals(BookCopyStatus.ON_LOAN, copy.getStatus());

        verify(bookCopyDAO).save(copy);
        verify(borrowRequestDAO).save(request);
    }

    @Test
    void approvalWithoutAvailableCopyLeavesRequestPending() {
        authenticate(librarian);
        BorrowRequest request = pendingRequest(11, member, book);
        when(borrowRequestDAO.lockById(11)).thenReturn(Optional.of(request));
        when(loanDAO.countActiveByMember(7)).thenReturn(0L);
        when(bookCopyDAO.lockFirstAvailableForBook(3)).thenReturn(Optional.empty());

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.approveRequest(11));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("No available copy for this book. The request remains pending.", exception.getMessage());
        assertEquals(BorrowRequestStatus.PENDING, request.getStatus());
        verify(loanDAO, never()).saveAndFlush(any(Loan.class));
        verify(borrowRequestDAO, never()).save(any(BorrowRequest.class));
    }

    @Test
    void approvalCannotHappenTwice() {
        authenticate(librarian);
        BorrowRequest request = pendingRequest(11, member, book);
        request.setStatus(BorrowRequestStatus.APPROVED);
        when(borrowRequestDAO.lockById(11)).thenReturn(Optional.of(request));

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.approveRequest(11));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(bookCopyDAO, never()).lockFirstAvailableForBook(3);
        verify(loanDAO, never()).saveAndFlush(any(Loan.class));
    }

    @Test
    void approvalFailsWhenMemberReachedLoanLimit() {
        authenticate(librarian);
        BorrowRequest request = pendingRequest(11, member, book);
        when(borrowRequestDAO.lockById(11)).thenReturn(Optional.of(request));
        when(loanDAO.countActiveByMember(7)).thenReturn((long) MAX_LOANS);

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.approveRequest(11));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(bookCopyDAO, never()).lockFirstAvailableForBook(3);
    }

    @Test
    void approvalFailsWhenRequestingUserIsNoLongerAMember() {
        authenticate(librarian);
        Users changed = user(7, "Changed", "member@example.test", Role.LIBRARIAN);
        BorrowRequest request = pendingRequest(11, changed, book);
        when(borrowRequestDAO.lockById(11)).thenReturn(Optional.of(request));

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.approveRequest(11));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(bookCopyDAO, never()).lockFirstAvailableForBook(3);
    }

    @Test
    void failureAfterLoanFlushDoesNotMutateCopyOrRequest() {
        authenticate(librarian);
        BorrowRequest request = pendingRequest(11, member, book);
        BookCopy copy = copy(21, book, "BC-21");
        when(borrowRequestDAO.lockById(11)).thenReturn(Optional.of(request));
        when(loanDAO.countActiveByMember(7)).thenReturn(0L);
        when(bookCopyDAO.lockFirstAvailableForBook(3)).thenReturn(Optional.of(copy));
        when(loanDAO.saveAndFlush(any(Loan.class)))
                .thenThrow(new DataIntegrityViolationException("uq_loan_active_copy"));

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.approveRequest(11));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
        assertEquals(BorrowRequestStatus.PENDING, request.getStatus());
        verify(bookCopyDAO, never()).save(any(BookCopy.class));
        verify(borrowRequestDAO, never()).save(any(BorrowRequest.class));
    }

    @Test
    void memberCannotApproveRequests() {
        authenticate(member);

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.approveRequest(11));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(borrowRequestDAO, never()).lockById(11);
    }

    // ------------------------------------------------------------------
    // Rejection
    // ------------------------------------------------------------------

    @Test
    void staffCanRejectPendingRequest() {
        authenticate(librarian);
        BorrowRequest request = pendingRequest(11, member, book);
        when(borrowRequestDAO.lockById(11)).thenReturn(Optional.of(request));

        BorrowRequestResponse response = service.rejectRequest(11);

        assertEquals("REJECTED", response.getStatus());
        assertEquals(BorrowRequestStatus.REJECTED, request.getStatus());
        assertEquals(9, request.getReviewedBy().getId());
        assertEquals(FIXED_NOW, request.getReviewedAt());
        verify(borrowRequestDAO).save(request);
    }

    @Test
    void approvedRequestCannotBeRejected() {
        authenticate(librarian);
        BorrowRequest request = pendingRequest(11, member, book);
        request.setStatus(BorrowRequestStatus.APPROVED);
        when(borrowRequestDAO.lockById(11)).thenReturn(Optional.of(request));

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.rejectRequest(11));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(borrowRequestDAO, never()).save(any(BorrowRequest.class));
    }

    // ------------------------------------------------------------------
    // Returns
    // ------------------------------------------------------------------

    @Test
    void returnMarksLoanReturnedAndCopyAvailable() {
        authenticate(librarian);
        BookCopy copy = copy(21, book, "BC-21");
        Loan loan = activeLoan(31, copy, member);
        when(loanDAO.lockById(31)).thenReturn(Optional.of(loan));
        when(bookCopyDAO.lockById(21)).thenReturn(Optional.of(copy));

        LoanResponse response = service.returnLoan(31);

        assertEquals("RETURNED", response.getStatus());
        assertEquals(FIXED_NOW, response.getReturnedAt());
        assertEquals(LoanStatus.RETURNED, loan.getStatus());
        assertEquals(FIXED_NOW, loan.getReturnedAt());
        assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
        verify(loanDAO).save(loan);
        verify(bookCopyDAO).save(copy);
    }

    @Test
    void returnCannotHappenTwice() {
        authenticate(librarian);
        Loan loan = activeLoan(31, copy(21, book, "BC-21"), member);
        loan.setStatus(LoanStatus.RETURNED);
        when(loanDAO.lockById(31)).thenReturn(Optional.of(loan));

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.returnLoan(31));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(bookCopyDAO, never()).lockById(21);
        verify(bookCopyDAO, never()).save(any(BookCopy.class));
    }

    @Test
    void returningUnknownLoanIsNotFound() {
        authenticate(librarian);
        when(loanDAO.lockById(31)).thenReturn(Optional.empty());

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> service.returnLoan(31));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void memberCannotListActiveLoansOrReturnBooks() {
        authenticate(member);

        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(BorrowingException.class, () -> service.getActiveLoans()).getStatus());
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(BorrowingException.class, () -> service.returnLoan(31)).getStatus());
    }

    // ------------------------------------------------------------------
    // Lists
    // ------------------------------------------------------------------

    @Test
    void memberSeesOwnRequestHistory() {
        authenticate(member);
        BorrowRequest pending = pendingRequest(11, member, book);
        BorrowRequest rejected = pendingRequest(12, member, book);
        rejected.setStatus(BorrowRequestStatus.REJECTED);
        when(borrowRequestDAO.findByMember(7)).thenReturn(List.of(rejected, pending));

        List<BorrowRequestResponse> response = service.getMyRequests();

        assertEquals(2, response.size());
        assertEquals("REJECTED", response.get(0).getStatus());
        assertEquals(12, response.get(0).getRequestId());
    }

    @Test
    void staffSeesPendingRequests() {
        authenticate(librarian);
        when(borrowRequestDAO.findPending()).thenReturn(List.of(pendingRequest(11, member, book)));

        List<BorrowRequestResponse> response = service.getPendingRequests();

        assertEquals(1, response.size());
        assertNotNull(response.get(0).getBookTitle());
    }

    @Test
    void staffSeesActiveLoansIncludingLegacyLoans() {
        authenticate(librarian);
        BookCopy legacyCopy = copy(21, book, "LEGACY-3");
        legacyCopy.setStatus(BookCopyStatus.ON_LOAN);
        Loan legacy = activeLoan(31, legacyCopy, member);
        legacy.setLegacy(true);
        when(loanDAO.findActiveLoans()).thenReturn(List.of(legacy));

        List<LoanResponse> response = service.getActiveLoans();

        assertEquals(1, response.size());
        assertEquals(true, response.get(0).isLegacy());
        assertEquals(BookCopyStatus.ON_LOAN, legacy.getCopy().getStatus());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private BorrowRequestCreateRequest requestFor(Integer bookId) {
        BorrowRequestCreateRequest body = new BorrowRequestCreateRequest();
        body.setBookId(bookId);
        return body;
    }

    private BorrowRequest pendingRequest(int id, Users owner, Book targetBook) {
        BorrowRequest request = new BorrowRequest(targetBook, owner, FIXED_NOW.minusSeconds(60));
        request.setId(id);
        return request;
    }

    private Loan activeLoan(int id, BookCopy copy, Users owner) {
        Loan loan = new Loan();
        loan.setId(id);
        loan.setCopy(copy);
        loan.setMember(owner);
        loan.setIssueDate(FIXED_NOW.minusSeconds(86_400));
        loan.setDueDate(FIXED_NOW.plusSeconds(13L * 24 * 60 * 60));
        loan.setStatus(LoanStatus.ACTIVE);
        return loan;
    }

    private Book book(int id, String name) {
        Book target = new Book();
        target.setId(id);
        target.setName(name);
        target.setPrice(10.0);
        return target;
    }

    private BookCopy copy(int id, Book targetBook, String code) {
        BookCopy copy = new BookCopy(targetBook, code, BookCopyStatus.AVAILABLE);
        copy.setId(id);
        return copy;
    }

    private Users user(int id, String name, String email, Role role) {
        Users user = new Users();
        user.setId(id);
        user.setName(name);
        user.setRole(role);
        Profile profile = new Profile();
        profile.setEmail(email);
        user.setProfile(profile);
        return user;
    }

    private void authenticate(Users user) {
        when(usersDAO.findByEmail(user.getProfile().getEmail())).thenReturn(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getProfile().getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
