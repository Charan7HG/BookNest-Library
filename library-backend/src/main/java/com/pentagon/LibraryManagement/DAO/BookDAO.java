package com.pentagon.LibraryManagement.DAO;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.pentagon.LibraryManagement.Repository.BookRepository;
import com.pentagon.LibraryManagement.entity.Book;

@Repository
public class BookDAO {
	@Autowired
	private BookRepository book;

	public Book addBook(Book b) {
		// book.user_id is legacy data owned by V2. New books never assign a borrower here.
		b.setUsers(null);
		return book.save(b);
	}

	public List<Book> getAllBooks() {
		return book.findAll();
	}

	public Book findBookById(int b_id) {
		// Read-only lookup: this must never clear an existing legacy borrower.
		Optional<Book> booko = book.findById(b_id);
		return booko.orElse(null);
	}

	public Book getById(int id) {
		Optional<Book> o = book.findById(id);
		return o.orElse(null);
	}

	public Book deleteBook(int id) {
		Optional<Book> o = book.findById(id);
		if (o.isPresent()) {
			book.deleteById(id);
			return o.get();
		}
		return null;
	}

	public Book updateBook(int id, Book updatedBook) {

		Optional<Book> optional = book.findById(id);

		if (optional.isPresent()) {

			Book existingBook = optional.get();

			existingBook.setName(updatedBook.getName());
			existingBook.setPrice(updatedBook.getPrice());
			existingBook.setAuthor(updatedBook.getAuthor());

			// Only replace optional metadata when the caller actually supplied it,
			// so a partial update cannot silently erase existing values.
			if (updatedBook.getDescription() != null) {
				existingBook.setDescription(updatedBook.getDescription());
			}
			if (updatedBook.getPublishedYear() != null) {
				existingBook.setPublishedYear(updatedBook.getPublishedYear());
			}
			if (updatedBook.getLanguage() != null) {
				existingBook.setLanguage(updatedBook.getLanguage());
			}

			// book.user_id is legacy data and must not be rewritten through the catalog API.
			existingBook.setCategory(updatedBook.getCategory());

			return book.save(existingBook);

		}
		return null;
	}

	public List<Book> searchByName(String name) {
		return book.findByNameContainingIgnoreCase(name);
	}
}
