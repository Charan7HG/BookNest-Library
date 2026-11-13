package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.pentagon.LibraryManagement.Repository.AuthorRepository;
import com.pentagon.LibraryManagement.Repository.BookCopyRepository;
import com.pentagon.LibraryManagement.Repository.BookRepository;
import com.pentagon.LibraryManagement.Repository.BorrowRequestRepository;
import com.pentagon.LibraryManagement.Repository.CategoryRepository;
import com.pentagon.LibraryManagement.Repository.LoanRepository;
import com.pentagon.LibraryManagement.Repository.UserRepository;
import com.pentagon.LibraryManagement.entity.Author;
import com.pentagon.LibraryManagement.entity.Book;
import com.pentagon.LibraryManagement.entity.BookCopy;
import com.pentagon.LibraryManagement.entity.BookCopyStatus;
import com.pentagon.LibraryManagement.entity.BorrowRequest;
import com.pentagon.LibraryManagement.entity.BorrowRequestStatus;
import com.pentagon.LibraryManagement.entity.Category;
import com.pentagon.LibraryManagement.entity.Loan;
import com.pentagon.LibraryManagement.entity.LoanStatus;
import com.pentagon.LibraryManagement.entity.Profile;
import com.pentagon.LibraryManagement.entity.Role;
import com.pentagon.LibraryManagement.entity.Users;
import com.pentagon.LibraryManagement.security.JwtUtil;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:librarybookcopy;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
@Transactional
class BookCreationIntegrationTests {

	private static final String PASSWORD = "original-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuthorRepository authorRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private BookCopyRepository bookCopyRepository;

	@Autowired
	private BorrowRequestRepository borrowRequestRepository;

	@Autowired
	private LoanRepository loanRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtUtil jwtUtil;

	private Users admin;
	private Users librarian;
	private Users member;
	private Author author;
	private Category category;

	@BeforeEach
	void setUp() {
		String suffix = UUID.randomUUID().toString();
		admin = account("admin-" + suffix + "@example.test", Role.ADMIN);
		librarian = account("librarian-" + suffix + "@example.test", Role.LIBRARIAN);
		member = account("member-" + suffix + "@example.test", Role.MEMBER);

		author = new Author();
		author.setName("Author " + suffix);
		author = authorRepository.saveAndFlush(author);

		category = new Category();
		category.setName("Category " + suffix);
		category = categoryRepository.saveAndFlush(category);
	}

	@Test
	void creatingABookAlsoCreatesItsAvailablePhysicalCopy() throws Exception {
		MvcResult created = mockMvc.perform(post("/book")
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(bookJson("Java Programming", 1)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.name").value("Java Programming"))
			.andExpect(jsonPath("$.data.description").value("A test description"))
			.andExpect(jsonPath("$.data.publishedYear").value(2024))
			.andExpect(jsonPath("$.data.language").value("English"))
			.andExpect(jsonPath("$.data.totalCopies").value(1))
			.andExpect(jsonPath("$.data.availableCopies").value(1))
			.andExpect(jsonPath("$.data.onLoanCopies").value(0))
			.andReturn();

		int bookId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");
		List<BookCopy> copies = bookCopyRepository.findAll().stream()
				.filter(copy -> copy.getBook().getId() == bookId)
				.toList();

		assertEquals(1, copies.size());
		assertEquals("BK-" + bookId + "-1", copies.get(0).getCopyCode());
		assertEquals(BookCopyStatus.AVAILABLE, copies.get(0).getStatus());
	}

	@Test
	void multipleCopiesHaveUniqueSequentialCodesAndRealCounts() throws Exception {
		int bookId = createBook(admin, "Clean Code", 3);

		List<BookCopy> copies = bookCopyRepository.findAll().stream()
				.filter(copy -> copy.getBook().getId() == bookId)
				.toList();
		Set<String> codes = copies.stream().map(BookCopy::getCopyCode).collect(Collectors.toSet());

		assertEquals(3, copies.size());
		assertEquals(3, codes.size(), "Every copy code must be unique");
		assertTrue(codes.containsAll(Set.of(
				"BK-" + bookId + "-1", "BK-" + bookId + "-2", "BK-" + bookId + "-3")));
		assertTrue(copies.stream().allMatch(copy -> copy.getStatus() == BookCopyStatus.AVAILABLE));

		mockMvc.perform(get("/books").header("Authorization", bearer(member)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].id").value(bookId))
			.andExpect(jsonPath("$.data[0].totalCopies").value(3))
			.andExpect(jsonPath("$.data[0].availableCopies").value(3))
			.andExpect(jsonPath("$.data[0].onLoanCopies").value(0));
	}

	@Test
	void invalidCopyQuantityIsRejectedWithoutCreatingABook() throws Exception {
		long booksBefore = bookRepository.count();

		for (int copies : new int[] {0, -1}) {
			mockMvc.perform(post("/book")
					.header("Authorization", bearer(librarian))
					.contentType(MediaType.APPLICATION_JSON)
					.content(bookJson("Invalid Copies", copies)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("Number of copies must be a positive whole number"));
		}

		mockMvc.perform(post("/book")
				.header("Authorization", bearer(librarian))
				.contentType(MediaType.APPLICATION_JSON)
				.content(bookJson("Missing Copies", null)))
			.andExpect(status().isBadRequest());

		assertEquals(booksBefore, bookRepository.count());
		assertEquals(0, bookCopyRepository.count());
	}

	@Test
	void unknownAuthorOrCategoryIsRejected() throws Exception {
		mockMvc.perform(post("/book")
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(bookJson("Unknown Author", 1, Integer.MAX_VALUE, category.getId())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Author not found"));

		mockMvc.perform(post("/book")
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(bookJson("Unknown Category", 1, author.getId(), Integer.MAX_VALUE)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Category not found"));
	}

	@Test
	void creatingNewBooksNeverChangesLegacyCopyCodes() throws Exception {
		Book legacyBook = new Book();
		legacyBook.setName("Legacy Book");
		legacyBook.setPrice(1.0);
		legacyBook = bookRepository.saveAndFlush(legacyBook);
		BookCopy legacyCopy = bookCopyRepository.saveAndFlush(
				new BookCopy(legacyBook, "LEGACY-" + legacyBook.getId(), BookCopyStatus.AVAILABLE));

		int newBookId = createBook(admin, "New Book", 2);

		BookCopy reloadedLegacy = bookCopyRepository.findById(legacyCopy.getId()).orElseThrow();
		assertEquals("LEGACY-" + legacyBook.getId(), reloadedLegacy.getCopyCode());
		assertEquals(BookCopyStatus.AVAILABLE, reloadedLegacy.getStatus());
		assertFalse(bookCopyRepository.findAll().stream()
				.anyMatch(copy -> copy.getBook().getId() == newBookId
						&& copy.getCopyCode().startsWith("LEGACY-")));
	}

	@Test
	void bookWithEmptyCopiesCanBeDeletedButLoanHistoryAndPendingRequestsBlockIt() throws Exception {
		int deletableId = createBook(admin, "Deletable", 2);
		mockMvc.perform(delete("/book/{id}", deletableId).header("Authorization", bearer(admin)))
			.andExpect(status().isOk());
		assertFalse(bookRepository.existsById(deletableId));
		assertEquals(0, bookCopyRepository.findAll().stream()
				.filter(copy -> copy.getBook().getId() == deletableId).count());

		int pendingId = createBook(admin, "Pending Request", 1);
		Book pendingBook = bookRepository.findById(pendingId).orElseThrow();
		borrowRequestRepository.saveAndFlush(new BorrowRequest(pendingBook, member, Instant.now()));

		mockMvc.perform(delete("/book/{id}", pendingId).header("Authorization", bearer(admin)))
			.andExpect(status().isConflict());
		assertTrue(bookRepository.existsById(pendingId));

		int loanedId = createBook(admin, "Loan History", 1);
		Book loanedBook = bookRepository.findById(loanedId).orElseThrow();
		BookCopy copy = bookCopyRepository.findAll().stream()
				.filter(bookCopy -> bookCopy.getBook().getId() == loanedId)
				.findFirst()
				.orElseThrow();
		Loan loan = new Loan();
		loan.setCopy(copy);
		loan.setMember(member);
		loan.setIssueDate(Instant.now());
		loan.setDueDate(Instant.now().plusSeconds(14L * 24 * 60 * 60));
		loan.setReturnedAt(Instant.now());
		loan.setStatus(LoanStatus.RETURNED);
		loanRepository.saveAndFlush(loan);

		mockMvc.perform(delete("/book/{id}", loanedId).header("Authorization", bearer(admin)))
			.andExpect(status().isConflict());
		assertTrue(bookRepository.existsById(loanedId));
		assertTrue(loanRepository.existsById(loan.getId()), "Loan history must never be deleted");
	}

	@Test
	void bookCreatedThroughTheApiCanBeRequestedApprovedAndReturned() throws Exception {
		int bookId = createBook(admin, "Borrowable", 1);

		MvcResult created = mockMvc.perform(post("/borrow-requests")
				.header("Authorization", bearer(member))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"bookId\":" + bookId + "}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.status").value("PENDING"))
			.andReturn();

		int requestId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.requestId");

		MvcResult approved = mockMvc.perform(post("/borrow-requests/{id}/approve", requestId)
				.header("Authorization", bearer(librarian)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("APPROVED"))
			.andExpect(jsonPath("$.data.loanId").isNumber())
			.andReturn();

		int loanId = JsonPath.read(approved.getResponse().getContentAsString(), "$.data.loanId");
		BorrowRequest request = borrowRequestRepository.findById(requestId).orElseThrow();
		Loan loan = loanRepository.findById(loanId).orElseThrow();

		assertEquals(BorrowRequestStatus.APPROVED, request.getStatus());
		assertEquals(LoanStatus.ACTIVE, loan.getStatus());
		assertEquals(BookCopyStatus.ON_LOAN, loan.getCopy().getStatus());
		assertEquals(1, bookCopyRepository.findAll().stream()
				.filter(copy -> copy.getBook().getId() == bookId && copy.getStatus() == BookCopyStatus.ON_LOAN)
				.count());

		mockMvc.perform(post("/loans/{id}/return", loanId)
				.header("Authorization", bearer(librarian)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("RETURNED"));

		assertEquals(BookCopyStatus.AVAILABLE,
				bookCopyRepository.findById(loan.getCopy().getId()).orElseThrow().getStatus());
	}

	private int createBook(Users user, String name, int copies) throws Exception {
		MvcResult result = mockMvc.perform(post("/book")
				.header("Authorization", bearer(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(bookJson(name, copies)))
			.andExpect(status().isCreated())
			.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
	}

	private String bookJson(String name, Integer copies) {
		return bookJson(name, copies, author.getId(), category.getId());
	}

	private String bookJson(String name, Integer copies, int authorId, int categoryId) {
		String copiesField = copies == null ? "null" : copies.toString();
		return "{\"name\":\"" + name + "\",\"description\":\"A test description\","
				+ "\"price\":19.95,\"publishedYear\":2024,\"language\":\"English\","
				+ "\"authorId\":" + authorId + ",\"categoryId\":" + categoryId
				+ ",\"numberOfCopies\":" + copiesField + "}";
	}

	private Users account(String email, Role role) {
		Profile profile = new Profile();
		profile.setEmail(email);
		profile.setPhone(123456789L);
		Users user = new Users();
		user.setName(role.name() + " Test Account");
		user.setProfile(profile);
		user.setRole(role);
		user.setPassword(passwordEncoder.encode(PASSWORD));
		return userRepository.saveAndFlush(user);
	}

	private String bearer(Users user) {
		return "Bearer " + jwtUtil.generateToken(user.getProfile().getEmail(), user.getRole().name());
	}
}
