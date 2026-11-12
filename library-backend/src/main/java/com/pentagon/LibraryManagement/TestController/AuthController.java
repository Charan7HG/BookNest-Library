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
import com.pentagon.LibraryManagement.Service.AuthorService;
import com.pentagon.LibraryManagement.entity.Author;
import com.pentagon.LibraryManagement.entity.Book;

@RestController
public class AuthController {
	@Autowired
	private AuthorService au;

	@PostMapping("/author")
	public ResponseEntity<ResponseStructure<Author>> AddAuthor(@RequestBody Author a){

		return au.addAuthor(a);
	}

	@GetMapping("/authors")
	public ResponseEntity<ResponseStructure<List<Author>>> getAllAuthor(){

		return au.getAllAuthor();
	}

	@GetMapping("/author/books/{id}")
	public ResponseEntity<ResponseStructure<List<Book>>>getBooks(@PathVariable int id){

		return au.getBooks(id);
	}

	@GetMapping("/author/{id}")
public ResponseEntity<ResponseStructure<Author>>findAuthorById(@PathVariable int id){

		return au.findAuthorById(id);
	}

	@GetMapping("/author/search")
public ResponseEntity<ResponseStructure<List<Author>>>searchByName(@RequestParam	 String name){

		return au.searchByName(name);
	}


	@PutMapping("/author/{id}")
public ResponseEntity<ResponseStructure<Author>>updateAuthor(@PathVariable int id, @RequestBody Author author){

		return au.updateAuthor(id, author);
	}


	@DeleteMapping("/author/{id}")
public ResponseEntity<ResponseStructure<Boolean>> deleteAuthor(@PathVariable int id){

		return au.deleteAuthor(id);
	}
}
