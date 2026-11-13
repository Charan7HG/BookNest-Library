package com.pentagon.LibraryManagement.DAO;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pentagon.LibraryManagement.Repository.AuthorRepository;
import com.pentagon.LibraryManagement.Repository.BookRepository;
import com.pentagon.LibraryManagement.entity.Author;
import com.pentagon.LibraryManagement.entity.Book;

@Repository
public class AuthorDAO {
	 @Autowired
	    private AuthorRepository author;
	 @Autowired
	    private BookRepository books;

	    public Author addAuthor(Author a) {
	        return author.save(a);
	    }

	    public Optional<Author> findByNameIgnoreCase(String name) {
	        return author.findFirstByNameIgnoreCase(name);
	    }

	    public List<Author> getAllAuthor() {
	        return author.findAll();
	    }




	    public List<Book> getBooks(int id) {

	        Optional<Author> optional = author.findById(id);

	        if(optional.isPresent()){

	            return optional.get().getBooks();

	        }

	        return null;

	    }

	    public Author getAuthorById(int id) {

	        Optional<Author> optional = author.findById(id);

	        return optional.orElse(null);

	    }
	    public List<Author> searchByName(String name) {

	        return author.findByNameContainingIgnoreCase(name);

	    }
	    public Author updateAuthor(int id, Author updatedAuthor) {

	        Optional<Author> optional = author.findById(id);

	        if(optional.isPresent()){

	            Author existingAuthor = optional.get();

	            existingAuthor.setName(updatedAuthor.getName());

	            return author.save(existingAuthor);

	        }

	        return null;

	    }
    @Transactional
    public boolean deleteAuthor(int id) {

	        Optional<Author> optional = author.findById(id);

	        if(optional.isPresent()){

                Author existingAuthor = optional.get();
                books.findByAuthor_Id(id).forEach(book -> book.setAuthor(null));
                author.delete(existingAuthor);

	            return true;

	        }

	        return false;

	    }


}
