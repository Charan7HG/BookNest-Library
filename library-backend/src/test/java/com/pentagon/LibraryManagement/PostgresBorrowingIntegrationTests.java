package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

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
import com.pentagon.LibraryManagement.entity.LoanStatus;
import com.pentagon.LibraryManagement.entity.Profile;
import com.pentagon.LibraryManagement.entity.Role;
import com.pentagon.LibraryManagement.entity.Users;
import com.pentagon.LibraryManagement.exception.BorrowingException;

/**
 * Real PostgreSQL integration tests.
 *
 * Disabled unless LIBRARY_PG_IT_URL points at a disposable, empty database.
 * The database must never be the real Library database: the test truncates
 * its borrowing tables between tests.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "LIBRARY_PG_IT_URL", matches = ".+")
class PostgresBorrowingIntegrationTests {

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("LIBRARY_PG_IT_URL"));
        registry.add("spring.datasource.username",
                () -> System.getenv().getOrDefault("LIBRARY_PG_IT_USERNAME", "postgres"));
        registry.add("spring.datasource.password",
                () -> System.getenv().getOrDefault("LIBRARY_PG_IT_PASSWORD", ""));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // src/test/resources sets H2Dialect for the in-memory suite; this test must use PostgreSQL.
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("app.borrowing.loan-period-days", () -> "14");
        registry.add("app.borrowing.max-active-loans", () -> "3");
        registry.add("app.borrowing.timezone", () -> "Asia/Kolkata");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private Users member;
    private Users librarian;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.execute("truncate table password_reset_request, borrow_request, loan, book_copy, book, "
                + "book_category, users, profile restart identity cascade");

        String suffix = UUID.randomUUID().toString();
        member = account("member-" + suffix + "@example.test", Role.MEMBER);
        librarian = account("librarian-" + suffix + "@example.test", Role.LIBRARIAN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void flywayMigratesAllVersionsAndHibernateValidatesTheSchema() {
        List<String> versions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true order by installed_rank",
                String.class);
        assertEquals(List.of("1", "2", "3", "4", "5"), versions);

        for (String table : List.of("book_copy", "loan", "borrow_request", "password_reset_request")) {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables "
                            + "where table_schema = 'public' and table_name = ?",
                    Integer.class, table);
            assertNotNull(count);
            assertEquals(1, count, "Missing table: " + table);
        }

        Integer passwordChangeColumn = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = 'public' and table_name = 'users' "
                        + "and column_name = 'password_change_required'",
                Integer.class);
        assertNotNull(passwordChangeColumn);
        assertEquals(1, passwordChangeColumn, "Missing users.password_change_required column");

        String indexDefinition = jdbcTemplate.queryForObject(
                "select indexdef from pg_indexes "
                        + "where schemaname = 'public' and indexname = 'uq_borrow_request_pending_member_book'",
                String.class);
        assertNotNull(indexDefinition);
        assertTrue(indexDefinition.contains("UNIQUE"));
        assertTrue(indexDefinition.toUpperCase().contains("WHERE"));
        assertTrue(indexDefinition.toUpperCase().contains("PENDING"));

        String loanIndex = jdbcTemplate.queryForObject(
                "select indexdef from pg_indexes "
                        + "where schemaname = 'public' and indexname = 'uq_loan_active_copy'",
                String.class);
        assertNotNull(loanIndex);
        assertTrue(loanIndex.toUpperCase().contains("WHERE"));
        assertTrue(loanIndex.toUpperCase().contains("ACTIVE"));

        String passwordResetIndex = jdbcTemplate.queryForObject(
                "select indexdef from pg_indexes "
                        + "where schemaname = 'public' "
                        + "and indexname = 'uq_password_reset_request_pending_user'",
                String.class);
        assertNotNull(passwordResetIndex);
        assertTrue(passwordResetIndex.contains("UNIQUE"));
        assertTrue(passwordResetIndex.toUpperCase().contains("WHERE"));
        assertTrue(passwordResetIndex.toUpperCase().contains("PENDING"));
    }

    @Test
    void postgresPartialUniqueIndexRejectsDuplicatePendingRequestsButKeepsHistory() {
        Book book = book("Postgres Index");
        BorrowRequest first = borrowRequestRepository.saveAndFlush(
                new BorrowRequest(book, member, Instant.now()));

        assertThrows(DataIntegrityViolationException.class,
                () -> borrowRequestRepository.saveAndFlush(
                        new BorrowRequest(book, member, Instant.now())));

        first.setStatus(BorrowRequestStatus.REJECTED);
        borrowRequestRepository.saveAndFlush(first);

        BorrowRequest second = borrowRequestRepository.saveAndFlush(
                new BorrowRequest(book, member, Instant.now()));

        assertNotNull(second.getId());
        assertEquals(2, borrowRequestRepository.count());
        assertEquals(1, borrowRequestRepository
                .findByStatusOrderByRequestedAtAsc(BorrowRequestStatus.PENDING).size());
    }

    @Test
    void concurrentApprovalsOnPostgresAllocateOnlyOneCopy() throws Exception {
        Users otherMember = account("other-" + UUID.randomUUID() + "@example.test", Role.MEMBER);
        Book book = book("Postgres Contention");
        BookCopy copy = bookCopyRepository.save(new BookCopy(book, "PG-1", BookCopyStatus.AVAILABLE));

        BorrowRequest first = borrowRequestRepository.saveAndFlush(
                new BorrowRequest(book, member, Instant.now()));
        BorrowRequest second = borrowRequestRepository.saveAndFlush(
                new BorrowRequest(book, otherMember, Instant.now()));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstResult = executor.submit(approvalTask(first.getId(), start));
            Future<Boolean> secondResult = executor.submit(approvalTask(second.getId(), start));

            start.countDown();

            boolean firstSucceeded = firstResult.get();
            boolean secondSucceeded = secondResult.get();

            assertTrue(firstSucceeded ^ secondSucceeded,
                    "PostgreSQL row locking must allow exactly one approval");
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, loanRepository.count());
        assertEquals(1, loanRepository.findByStatusOrderByIdAsc(LoanStatus.ACTIVE).size());
        assertEquals(BookCopyStatus.ON_LOAN,
                bookCopyRepository.findById(copy.getId()).orElseThrow().getStatus());

        List<BorrowRequest> requests = borrowRequestRepository.findAll();
        assertEquals(1, requests.stream()
                .filter(request -> request.getStatus() == BorrowRequestStatus.APPROVED).count());
        assertEquals(1, requests.stream()
                .filter(request -> request.getStatus() == BorrowRequestStatus.PENDING).count());
    }

    @Test
    void noCopyLeavesRequestPendingOnPostgres() {
        Book book = book("Postgres No Copy");
        BorrowRequest request = borrowRequestRepository.saveAndFlush(
                new BorrowRequest(book, member, Instant.now()));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        librarian.getProfile().getEmail(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))));

        BorrowingException exception = assertThrows(BorrowingException.class,
                () -> borrowRequestService.approveRequest(request.getId()));

        assertEquals(409, exception.getStatus().value());
        assertEquals(BorrowRequestStatus.PENDING,
                borrowRequestRepository.findById(request.getId()).orElseThrow().getStatus());
        assertEquals(0, loanRepository.count());
    }

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

    private Users account(String email, Role role) {
        Users user = new Users();
        user.setName(email);
        user.setRole(role);
        user.setPassword("test-password");
        Profile profile = new Profile();
        profile.setEmail(email);
        profile.setPhone(1000L);
        user.setProfile(profile);
        return userRepository.saveAndFlush(user);
    }

    private Book book(String name) {
        Book book = new Book();
        book.setName(name);
        book.setPrice(10.0);
        return bookRepository.saveAndFlush(book);
    }
}
