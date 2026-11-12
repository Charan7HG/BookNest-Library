package com.pentagon.LibraryManagement.TestController;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.Service.BookService;
import com.pentagon.LibraryManagement.entity.Book;

@RestController
public class BookController {

	@Autowired
	private BookService bs;

	@PostMapping("/book")
	public ResponseEntity<ResponseStructure<Book>>addBook(@RequestBody Book b){

		return bs.addBook(b);
	}

	@GetMapping("/books")
	public ResponseEntity<ResponseStructure<List<Book>>>getAllBooks(){

		return bs.getAllBooks();
	}

	@GetMapping("/bookbr")
	public ResponseEntity<ResponseStructure<Book>>  borrow(@RequestParam int b_id, @RequestParam int u_id){

		return bs.borrow(b_id, u_id);
	}

	@GetMapping("/book/{id}")
	public ResponseEntity<ResponseStructure<Book>>getById(@PathVariable int id){

		return bs.getById(id);
	}

	@DeleteMapping("/book/{id}")
	public ResponseEntity<ResponseStructure<Book>> deleteBook(@PathVariable int id){
		return bs.deleteBook(id);
	}

	@PutMapping("/book/{id}")
	public ResponseEntity<ResponseStructure<Book>> updateBook(
	        @PathVariable int id,
	        @RequestBody Book book) {

	    return bs.updateBook(id, book);
	}


	@GetMapping("/book/search")
	public ResponseEntity<ResponseStructure<List<Book>>> searchBook(
	        @RequestParam String name) {

	    return bs.searchByName(name);

	}
}
