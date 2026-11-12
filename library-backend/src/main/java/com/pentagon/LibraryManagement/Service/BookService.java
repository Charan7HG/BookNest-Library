package com.pentagon.LibraryManagement.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.pentagon.LibraryManagement.DAO.BookDAO;
import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.entity.Book;

@Service
public class BookService {

	@Autowired
	private BookDAO bd;

	public ResponseEntity<ResponseStructure<Book>> addBook(Book b) {
		Book book = bd.addBook(b);

		ResponseStructure<Book> response = new ResponseStructure<Book>();
		response.setStatuscode(HttpStatus.CREATED.value());
		response.setMessage("Book added...!");
		response.setData(book);
		return new ResponseEntity<ResponseStructure<Book>>(response, HttpStatus.CREATED);
	}

	public ResponseEntity<ResponseStructure<Book>> deleteBook(int b_id) {

		Book book = bd.deleteBook(b_id);

		ResponseStructure<Book> response = new ResponseStructure<>();

		if (book != null) {

			response.setStatuscode(HttpStatus.OK.value());
			response.setMessage("Book Deleted Successfully");
			response.setData(null);

			return new ResponseEntity<>(response, HttpStatus.OK);

		} else {

			response.setStatuscode(HttpStatus.NOT_FOUND.value());
			response.setMessage("Book Not Found");
			response.setData(null);

			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);

		}
	}

	public ResponseEntity<ResponseStructure<List<Book>>> getAllBooks() {
		List<Book> b = bd.getAllBooks();

		ResponseStructure<List<Book>> response = new ResponseStructure<List<Book>>();
		response.setStatuscode(HttpStatus.OK.value());
		response.setMessage("book fetched...!");
		response.setData(b);
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

	public ResponseEntity<ResponseStructure<Book>> findBookById(int b_id) {
		Book b = bd.findBookById(b_id);
		if (b != null) {
			ResponseStructure<Book> response = new ResponseStructure<Book>();
			response.setStatuscode(HttpStatus.OK.value());
			response.setMessage("books returned...!");
			response.setData(b);
			return new ResponseEntity<ResponseStructure<Book>>(response, HttpStatus.OK);
		} else {
			ResponseStructure<Book> response = new ResponseStructure<Book>();
			response.setStatuscode(HttpStatus.OK.value());
			response.setMessage("books not returned...!");
			response.setData(null);
			return new ResponseEntity<ResponseStructure<Book>>(response, HttpStatus.OK);
		}
	}

	public ResponseEntity<ResponseStructure<Book>> getById(int id) {
		Book b = bd.getById(id);
		if (b != null) {
			ResponseStructure<Book> response = new ResponseStructure<Book>();
			response.setStatuscode(HttpStatus.OK.value());
			response.setMessage("Book found!");
			response.setData(b);
			return new ResponseEntity<ResponseStructure<Book>>(response, HttpStatus.OK);

		} else {

			ResponseStructure<Book> response = new ResponseStructure<Book>();
			response.setStatuscode(HttpStatus.NOT_FOUND.value());
			response.setMessage("Book not found!");
			response.setData(null);
			return new ResponseEntity<ResponseStructure<Book>>(response, HttpStatus.NOT_FOUND);
		}
	}

	public ResponseEntity<ResponseStructure<Book>> updateBook(int id, Book book) {

		Book updatedBook = bd.updateBook(id, book);

		ResponseStructure<Book> response = new ResponseStructure<>();

		if (updatedBook != null) {

			response.setStatuscode(HttpStatus.OK.value());
			response.setMessage("Book Updated Successfully");
			response.setData(updatedBook);

			return new ResponseEntity<>(response, HttpStatus.OK);

		} else {

			response.setStatuscode(HttpStatus.NOT_FOUND.value());
			response.setMessage("Book Not Found");
			response.setData(null);

			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);

		}
	}

	public ResponseEntity<ResponseStructure<List<Book>>> searchByName(String name) {

		List<Book> books = bd.searchByName(name);

		ResponseStructure<List<Book>> response = new ResponseStructure<>();

		response.setStatuscode(HttpStatus.OK.value());
		response.setMessage("Books Found");
		response.setData(books);

		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	private ResponseEntity<ResponseStructure<Book>> bookResponse(HttpStatus status, String message, Book book) {
		ResponseStructure<Book> response = new ResponseStructure<Book>();
		response.setStatuscode(status.value());
		response.setMessage(message);
		response.setData(book);
		return new ResponseEntity<ResponseStructure<Book>>(response, status);
	}
}
