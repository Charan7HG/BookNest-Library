package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.pentagon.LibraryManagement.DAO.BorrowRequestDAO;
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
import com.pentagon.LibraryManagement.entity.Profile;
import com.pentagon.LibraryManagement.entity.Role;
import com.pentagon.LibraryManagement.entity.Users;

/**
 * Forces a failure after the loan and copy have been changed, then verifies
 * that the surrounding @Transactional approval rolled everything back.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:libraryrollback;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
@Import(BorrowingTransactionRollbackTests.FailingConfig.class)
class BorrowingTransactionRollbackTests {

    @TestConfiguration
    static class FailingConfig {

        @Bean
        @Primary
        FailingBorrowRequestDAO failingBorrowRequestDAO() {
            return new FailingBorrowRequestDAO();
        }
    }

    static class FailingBorrowRequestDAO extends BorrowRequestDAO {

        @Override
        public BorrowRequest save(BorrowRequest request) {
            throw new IllegalStateException("simulated failure after loan allocation");
        }
    }

    @Autowired
    private BorrowRequestService borrowRequestService;

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

    private Users member;
    private Users librarian;

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
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void failureAfterLoanAllocationRollsBackLoanCopyAndRequestChanges() {
        Book book = new Book();
        book.setName("Transactional");
        book.setPrice(10.0);
        bookRepository.save(book);

        BookCopy copy = bookCopyRepository.save(new BookCopy(book, "TX-1", BookCopyStatus.AVAILABLE));
        BorrowRequest request = borrowRequestRepository.save(
                new BorrowRequest(book, member, Instant.now()));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        librarian.getProfile().getEmail(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))));

        // Spring's persistence exception translation wraps the injected failure.
        assertThrows(RuntimeException.class, () -> borrowRequestService.approveRequest(request.getId()));

        assertEquals(0, loanRepository.count(), "The loan insert must be rolled back");
        assertEquals(BookCopyStatus.AVAILABLE,
                bookCopyRepository.findById(copy.getId()).orElseThrow().getStatus());
        BorrowRequest reloaded = borrowRequestRepository.findById(request.getId()).orElseThrow();
        assertEquals(BorrowRequestStatus.PENDING, reloaded.getStatus());
        assertEquals(null, reloaded.getLoan());
        assertEquals(null, reloaded.getReviewedBy());
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
}
