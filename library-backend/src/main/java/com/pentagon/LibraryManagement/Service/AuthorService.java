package com.pentagon.LibraryManagement.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.pentagon.LibraryManagement.DAO.AuthorDAO;
import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.entity.Author;
import com.pentagon.LibraryManagement.entity.Book;

@Service
public class AuthorService {
	@Autowired
	private AuthorDAO au;

	public ResponseEntity<ResponseStructure<Author>> addAuthor(Author a){
		Author author=au.addAuthor(a);

		ResponseStructure<Author> response = new ResponseStructure<Author>();
		response.setStatuscode(HttpStatus.CREATED.value());
		response.setMessage("author added...!");
		response.setData(author);
		return new ResponseEntity<ResponseStructure<Author>>(response,HttpStatus.CREATED);

	}

	public ResponseEntity<ResponseStructure<List<Author>>> getAllAuthor(){
		List<Author> a= au.getAllAuthor();

		ResponseStructure<List<Author>> response = new ResponseStructure<List<Author>>();
		response.setStatuscode(HttpStatus.OK.value());
		response.setMessage("author fetched...!");
		response.setData(a);
		return new ResponseEntity<ResponseStructure<List<Author>>>(response,HttpStatus.OK);

	}

	public ResponseEntity<ResponseStructure<List<Book>>> getBooks(int id){
		List<Book> books= au.getBooks(id);
		if(books != null) {
			ResponseStructure<List<Book>> response = new ResponseStructure<List<Book>>();
			response.setStatuscode(HttpStatus.OK.value());
			response.setMessage("books fetched...!");
			response.setData(books);
			return new ResponseEntity<ResponseStructure<List<Book>>>(response,HttpStatus.OK);

		}else {
			ResponseStructure<List<Book>> response = new ResponseStructure<List<Book>>();
			response.setStatuscode(HttpStatus.NOT_FOUND.value());
			response.setMessage("author or books not fetched...!");
			response.setData(books);
			return new ResponseEntity<ResponseStructure<List<Book>>>(response,HttpStatus.NOT_FOUND);
		}
	}




	public ResponseEntity<ResponseStructure<Author>> findAuthorById(int a_id){
		Author a= au.getAuthorById(a_id);
		if(a != null) {
			ResponseStructure<Author> response = new ResponseStructure<Author>();
			response.setStatuscode(HttpStatus.OK.value());
			response.setMessage("Authors returned...!");
			response.setData(a);
			return new ResponseEntity<ResponseStructure<Author>>(response,HttpStatus.OK);
		}else {
			ResponseStructure<Author> response = new ResponseStructure<Author>();
			response.setStatuscode(HttpStatus.NOT_FOUND.value());
			response.setMessage("Author Not Found");
			response.setData(null);
			return new ResponseEntity<ResponseStructure<Author>>(response,HttpStatus.NOT_FOUND);
		}
	}

	public ResponseEntity<ResponseStructure<List<Author>>> searchByName(String name) {

	    List<Author> authors = au.searchByName(name);

	    ResponseStructure<List<Author>> response = new ResponseStructure<>();

	    response.setStatuscode(HttpStatus.OK.value());
	    response.setMessage("Authors Found");
	    response.setData(authors);

	    return new ResponseEntity<>(response, HttpStatus.OK);

	}

	public ResponseEntity<ResponseStructure<Author>> updateAuthor( int id,Author author)  {

	    Author updatedAuthor = au.updateAuthor(id,author);

	    ResponseStructure<Author> response = new ResponseStructure<>();

	    if (updatedAuthor != null) {

	        response.setStatuscode(HttpStatus.OK.value());
	        response.setMessage("Author Updated Successfully");
	        response.setData(updatedAuthor);

	        return new ResponseEntity<>(response, HttpStatus.OK);

	    } else {

	        response.setStatuscode(HttpStatus.NOT_FOUND.value());
	        response.setMessage("Author Not Found");
	        response.setData(null);

	        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);

	    }
	}


	public ResponseEntity<ResponseStructure<Boolean>> deleteAuthor(int a_id) {

		   boolean author = au.deleteAuthor(a_id);

		    ResponseStructure<Boolean> response = new ResponseStructure<>();

		    if (author) {

		        response.setStatuscode(HttpStatus.OK.value());
		        response.setMessage("Author Deleted Successfully");
		        response.setData(author);

		        return new ResponseEntity<>(response, HttpStatus.OK);

		    } else {

		        response.setStatuscode(HttpStatus.NOT_FOUND.value());
		        response.setMessage("Author Not Found");
		        response.setData(null);

		        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);

		    }
		}
}
