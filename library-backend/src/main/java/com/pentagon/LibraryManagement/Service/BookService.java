package com.pentagon.LibraryManagement.Service;

import java.time.Year;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pentagon.LibraryManagement.DAO.AuthorDAO;
import com.pentagon.LibraryManagement.DAO.BookCopyDAO;
import com.pentagon.LibraryManagement.DAO.BookDAO;
import com.pentagon.LibraryManagement.DAO.BorrowRequestDAO;
import com.pentagon.LibraryManagement.DAO.CategoryDAO;
import com.pentagon.LibraryManagement.DAO.LoanDAO;
import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.dto.BookCreateRequest;
import com.pentagon.LibraryManagement.entity.Author;
import com.pentagon.LibraryManagement.entity.Book;
import com.pentagon.LibraryManagement.entity.BookCopy;
import com.pentagon.LibraryManagement.entity.BookCopyStatus;
import com.pentagon.LibraryManagement.entity.Category;
import com.pentagon.LibraryManagement.exception.BorrowingException;

@Service
public class BookService {

	private static final int EARLIEST_PUBLISHED_YEAR = 1000;

	@Autowired
	private BookDAO bd;

	@Autowired
	private BookCopyDAO bookCopyDAO;

	@Autowired
	private AuthorDAO authorDAO;

	@Autowired
	private CategoryDAO categoryDAO;

	@Autowired
	private BorrowRequestDAO borrowRequestDAO;

	@Autowired
	private LoanDAO loanDAO;

	/**
	 * Creates the catalogue book and its physical copies in one transaction.
	 * A failure while creating any copy rolls back the book as well.
	 */
	@Transactional
	public ResponseEntity<ResponseStructure<Book>> addBook(BookCreateRequest request) {
		validateNewBook(request);

		Author author = authorDAO.getAuthorById(request.getAuthorId());
		if (author == null) {
			throw new BorrowingException(HttpStatus.NOT_FOUND, "Author not found");
		}

		Category category = categoryDAO.getCategoryById(request.getCategoryId());
		if (category == null) {
			throw new BorrowingException(HttpStatus.NOT_FOUND, "Category not found");
		}

		Book book = new Book();
		book.setName(request.getName().trim());
		book.setDescription(trimToNull(request.getDescription()));
		book.setPrice(request.getPrice());
		book.setPublishedYear(request.getPublishedYear());
		book.setLanguage(request.getLanguage().trim());
		book.setAuthor(author);
		book.setCategory(List.of(category));

		Book saved;
		try {
			saved = bd.addBook(book);

			// Copy codes are derived from the generated book id. The id is unique,
			// so concurrent book creations cannot generate the same code, and the
			// database unique constraint on copy_code remains the final protection.
			for (int copyNumber = 1; copyNumber <= request.getNumberOfCopies(); copyNumber++) {
				String copyCode = "BK-" + saved.getId() + "-" + copyNumber;
				bookCopyDAO.save(new BookCopy(saved, copyCode, BookCopyStatus.AVAILABLE));
			}
		} catch (DataIntegrityViolationException exception) {
			throw new BorrowingException(HttpStatus.CONFLICT,
					"Could not create the book copies. Please try again.");
		}

		populateCopyCounts(saved);

		ResponseStructure<Book> response = new ResponseStructure<Book>();
		response.setStatuscode(HttpStatus.CREATED.value());
		response.setMessage("Book added...!");
		response.setData(saved);
		return new ResponseEntity<ResponseStructure<Book>>(response, HttpStatus.CREATED);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<Book>> deleteBook(int b_id) {

		Book book = bd.findBookById(b_id);

		if (book == null) {
			return bookResponse(HttpStatus.NOT_FOUND, "Book Not Found", null);
		}

		// Never delete a book while a member is waiting for a copy.
		if (borrowRequestDAO.existsPendingForBook(b_id)) {
			return bookResponse(HttpStatus.CONFLICT,
					"Book cannot be deleted while it has pending borrow requests", null);
		}

		// Loan rows are permanent history. A book with any loan record stays.
		if (loanDAO.existsAnyForBook(b_id)) {
			return bookResponse(HttpStatus.CONFLICT,
					"Book cannot be deleted because it has loan history", null);
		}

		try {
			bookCopyDAO.deleteAllForBook(b_id);
			bd.deleteBook(b_id);
		} catch (DataIntegrityViolationException exception) {
			return bookResponse(HttpStatus.CONFLICT,
					"Book cannot be deleted because related records still exist", null);
		}

		return bookResponse(HttpStatus.OK, "Book Deleted Successfully", null);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<Book>>> getAllBooks() {
		List<Book> books = bd.getAllBooks();
		populateCopyCounts(books);

		ResponseStructure<List<Book>> response = new ResponseStructure<List<Book>>();
		response.setStatuscode(HttpStatus.OK.value());
		response.setMessage("book fetched...!");
		response.setData(books);
		return new ResponseEntity<ResponseStructure<List<Book>>>(response, HttpStatus.OK);
	}

	/**
	 * Legacy direct-borrow endpoint. It is intentionally disabled: borrowing is
	 * now request-based, and this path must never mutate book.user_id again.
	 */
	public ResponseEntity<ResponseStructure<Book>> borrow(int b_id, int u_id) {
		ResponseStructure<Book> response = new ResponseStructure<Book>();
		response.setStatuscode(HttpStatus.GONE.value());
		response.setMessage("Direct borrowing is disabled. Submit a request via POST /borrow-requests");
		response.setData(null);
		return new ResponseEntity<ResponseStructure<Book>>(response, HttpStatus.GONE);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<Book>> findBookById(int b_id) {
		Book b = bd.findBookById(b_id);
		if (b != null) {
			populateCopyCounts(b);
			return bookResponse(HttpStatus.OK, "books returned...!", b);
		}
		return bookResponse(HttpStatus.OK, "books not returned...!", null);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<Book>> getById(int id) {
		Book b = bd.getById(id);
		if (b != null) {
			populateCopyCounts(b);
			return bookResponse(HttpStatus.OK, "Book found!", b);
		}
		return bookResponse(HttpStatus.NOT_FOUND, "Book not found!", null);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<Book>> updateBook(int id, Book book) {
		validateBookUpdate(book);

		Book updatedBook = bd.updateBook(id, book);
		if (updatedBook == null) {
			return bookResponse(HttpStatus.NOT_FOUND, "Book Not Found", null);
		}

		populateCopyCounts(updatedBook);
		return bookResponse(HttpStatus.OK, "Book Updated Successfully", updatedBook);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<Book>>> searchByName(String name) {
		List<Book> books = bd.searchByName(name);
		populateCopyCounts(books);
		return new ResponseEntity<ResponseStructure<List<Book>>>(
				listResponse(HttpStatus.OK, "Books Found", books), HttpStatus.OK);
	}

	private void validateNewBook(BookCreateRequest request) {
		if (request == null || request.getName() == null || request.getName().isBlank()) {
			throw new BorrowingException(HttpStatus.BAD_REQUEST, "Book name is required");
		}
		if (request.getPrice() == null || !Double.isFinite(request.getPrice()) || request.getPrice() < 0) {
			throw new BorrowingException(HttpStatus.BAD_REQUEST, "Price must be zero or greater");
		}
		validatePublishedYear(request.getPublishedYear());
		if (request.getLanguage() == null || request.getLanguage().isBlank()) {
			throw new BorrowingException(HttpStatus.BAD_REQUEST, "Language is required");
		}
		if (request.getAuthorId() == null) {
			throw new BorrowingException(HttpStatus.BAD_REQUEST, "Author is required");
		}
		if (request.getCategoryId() == null) {
			throw new BorrowingException(HttpStatus.BAD_REQUEST, "Category is required");
		}
		if (request.getNumberOfCopies() == null || request.getNumberOfCopies() < 1) {
			throw new BorrowingException(HttpStatus.BAD_REQUEST,
					"Number of copies must be a positive whole number");
		}
	}

	private void validateBookUpdate(Book book) {
		if (book == null || book.getName() == null || book.getName().isBlank()) {
			throw new BorrowingException(HttpStatus.BAD_REQUEST, "Book name is required");
		}
		if (!Double.isFinite(book.getPrice()) || book.getPrice() < 0) {
			throw new BorrowingException(HttpStatus.BAD_REQUEST, "Price must be zero or greater");
		}
		if (book.getPublishedYear() != null) {
			validatePublishedYear(book.getPublishedYear());
		}
	}

	private void validatePublishedYear(Integer publishedYear) {
		int latestYear = Year.now().getValue() + 1;
		if (publishedYear == null
				|| publishedYear < EARLIEST_PUBLISHED_YEAR
				|| publishedYear > latestYear) {
			throw new BorrowingException(HttpStatus.BAD_REQUEST,
					"Published year must be a valid year between " + EARLIEST_PUBLISHED_YEAR
							+ " and " + latestYear);
		}
	}

	private void populateCopyCounts(Book book) {
		book.setTotalCopies((int) bookCopyDAO.countTotalForBook(book.getId()));
		book.setAvailableCopies((int) bookCopyDAO.countForBookWithStatus(
				book.getId(), BookCopyStatus.AVAILABLE));
		book.setOnLoanCopies((int) bookCopyDAO.countForBookWithStatus(
				book.getId(), BookCopyStatus.ON_LOAN));
	}

	private void populateCopyCounts(List<Book> books) {
		for (Book book : books) {
			populateCopyCounts(book);
		}
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private ResponseEntity<ResponseStructure<Book>> bookResponse(HttpStatus status, String message, Book book) {
		ResponseStructure<Book> response = new ResponseStructure<Book>();
		response.setStatuscode(status.value());
		response.setMessage(message);
		response.setData(book);
		return new ResponseEntity<ResponseStructure<Book>>(response, status);
	}

	private ResponseStructure<List<Book>> listResponse(HttpStatus status, String message, List<Book> books) {
		ResponseStructure<List<Book>> response = new ResponseStructure<List<Book>>();
		response.setStatuscode(status.value());
		response.setMessage(message);
		response.setData(books);
		return response;
	}
}
