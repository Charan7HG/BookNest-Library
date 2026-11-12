package com.pentagon.LibraryManagement.DAO;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pentagon.LibraryManagement.Repository.UserRepository;
import com.pentagon.LibraryManagement.Repository.BookRepository;
import com.pentagon.LibraryManagement.entity.Book;
import com.pentagon.LibraryManagement.entity.Users;



@Repository
public class UsersDAO {
	@Autowired
	private UserRepository users;
	@Autowired
	private BookRepository books;
	public Users addUser(Users u) {
		return users.save(u);

	}
	public List<Users> getAllUser(){
		return users.findAll();
	}

	public Users getById(int id) {
		Optional<Users> o = users.findById(id);
		if(o.isPresent()) {
			return o.get();
		}else {
			return null;
		}
	}

	/**
	 * Locks a user row for the duration of the caller's transaction. Used to
	 * serialise password reset request submission for the same member.
	 */
	public Users lockById(int id) {
		Optional<Users> o = users.findByIdForUpdate(id);
		return o.orElse(null);
	}
	public Users update(Users u) {
		int id=u.getId();
		Optional<Users> o = users.findById(id);
		if(o.isPresent()) {
			return users.save(u);
		}else {
			return null;
		}
	}

	@Transactional
	public String delete(int id) {
		Optional<Users> o = users.findById(id);
		if(o.isPresent()) {
			Users u = o.get();
			List<Book> borrowedBooks = books.findByUsers_Id(id);
			borrowedBooks.forEach(book -> book.setUsers(null));
			users.delete(u);

			return "User Deleted";
			}else {
			return "User Not Deleted";
		}
	}


	public List<Users> searchByName(String name){

	    return users.findByNameContainingIgnoreCase(name);

	}
	public Users findByEmail(String email) {

	    return users.findByProfileEmail(email);

	}

}
