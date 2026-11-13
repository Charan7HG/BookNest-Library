package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.pentagon.LibraryManagement.DAO.BookCopyDAO;
import com.pentagon.LibraryManagement.Repository.AuthorRepository;
import com.pentagon.LibraryManagement.Repository.BookCopyRepository;
import com.pentagon.LibraryManagement.Repository.BookRepository;
import com.pentagon.LibraryManagement.Repository.BorrowRequestRepository;
import com.pentagon.LibraryManagement.Repository.CategoryRepository;
import com.pentagon.LibraryManagement.Repository.LoanRepository;
import com.pentagon.LibraryManagement.Repository.UserRepository;
import com.pentagon.LibraryManagement.Service.BookService;
import com.pentagon.LibraryManagement.dto.BookCreateRequest;
import com.pentagon.LibraryManagement.entity.Author;
import com.pentagon.LibraryManagement.entity.BookCopy;
import com.pentagon.LibraryManagement.entity.Category;

/**
 * Simulates a copy insert failure after the book was inserted and verifies
 * that the service transaction rolls the complete book creation back.
 */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:librarybookrollback;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
		"spring.flyway.enabled=false"
})
@Import(BookCreationRollbackTests.FailingConfig.class)
class BookCreationRollbackTests {

	@TestConfiguration
	static class FailingConfig {

		@Bean
		@Primary
		FailingBookCopyDAO failingBookCopyDAO() {
			return new FailingBookCopyDAO();
		}
	}

	static class FailingBookCopyDAO extends BookCopyDAO {

		@Override
		public BookCopy save(BookCopy copy) {
			throw new IllegalStateException("simulated copy creation failure");
		}
	}

	@Autowired
	private BookService bookService;

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
	private LoanRepository loanRepository;

	@Autowired
	private BorrowRequestRepository borrowRequestRepository;

	private Author author;
	private Category category;

	@BeforeEach
	void setUp() {
		borrowRequestRepository.deleteAll();
		loanRepository.deleteAll();
		bookCopyRepository.deleteAll();
		bookRepository.deleteAll();
		userRepository.deleteAll();
		authorRepository.deleteAll();
		categoryRepository.deleteAll();

		author = new Author();
		author.setName("Rollback Author");
		author = authorRepository.saveAndFlush(author);

		category = new Category();
		category.setName("Rollback Category");
		category = categoryRepository.saveAndFlush(category);
	}

	@Test
	void copyCreationFailureRollsBackTheBookAndAllCopies() {
		BookCreateRequest request = new BookCreateRequest();
		request.setName("Rolled Back Book");
		request.setDescription("Must not remain");
		request.setPrice(10.0);
		request.setPublishedYear(2024);
		request.setLanguage("English");
		request.setAuthorId(author.getId());
		request.setCategoryId(category.getId());
		request.setNumberOfCopies(3);

		assertThrows(RuntimeException.class, () -> bookService.addBook(request));

		assertEquals(0, bookRepository.count(), "The book insert must be rolled back");
		assertEquals(0, bookCopyRepository.count(), "No copy may remain after a failed creation");
	}
}
