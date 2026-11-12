package com.pentagon.LibraryManagement.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.pentagon.LibraryManagement.DAO.UsersDAO;
import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.entity.Users;

@Service
public class UsersService {

	@Autowired
	private UsersDAO ur;

	@Autowired
	private PasswordEncoder passwordEncoder;

	public ResponseEntity<ResponseStructure<Users>> addUser(Users u) {
		if (u == null || u.getProfile() == null || u.getProfile().getEmail() == null
				|| u.getProfile().getEmail().isBlank() || u.getPassword() == null || u.getPassword().isBlank()) {
			return userResponse(HttpStatus.BAD_REQUEST, "Email and password are required", null);
		}
		if (ur.findByEmail(u.getProfile().getEmail()) != null) {
			return userResponse(HttpStatus.CONFLICT, "Email already exists", null);
		}

		u.setPassword(passwordEncoder.encode(u.getPassword()));
		if (u.getRole() == null) {
			u.setRole(com.pentagon.LibraryManagement.entity.Role.MEMBER);
		}

		Users user;
		try {
			user = ur.addUser(u);
		} catch (DataIntegrityViolationException exception) {
			// A second request may claim the same unique profile email after the check above.
			return userResponse(HttpStatus.CONFLICT, "Email already exists", null);
		}

		return userResponse(HttpStatus.CREATED, "User Added Successfully", user);
	}

	public ResponseEntity<ResponseStructure<List<Users>>> getAllUser() {
		List<Users> user = ur.getAllUser();

		ResponseStructure<List<Users>> response = new ResponseStructure<List<Users>>();
		response.setStatuscode(HttpStatus.OK.value());
		response.setMessage("All Users Fetched Successfully");
		response.setData(user);
		return new ResponseEntity<ResponseStructure<List<Users>>>(response, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<Users>> findUserById(int id) {
		Users u = ur.getById(id);
		if (u != null) {
			ResponseStructure<Users> response = new ResponseStructure<Users>();
			response.setStatuscode(HttpStatus.OK.value());
			response.setMessage("User Found");
			response.setData(u);
			return new ResponseEntity<ResponseStructure<Users>>(response, HttpStatus.OK);
		} else {
			ResponseStructure<Users> response = new ResponseStructure<Users>();
			response.setStatuscode(HttpStatus.NOT_FOUND.value());
			response.setMessage("User not found");
			response.setData(null);
			return new ResponseEntity<ResponseStructure<Users>>(response, HttpStatus.NOT_FOUND);
		}
	}

	public ResponseEntity<ResponseStructure<Users>> updateUser(Users u) {
		Users existing = ur.getById(u.getId());
		if (existing != null) {
			// Preserve existing password if not provided in update request
			if (u.getPassword() == null || u.getPassword().isBlank()) {
				u.setPassword(existing.getPassword());
			} else {
				u.setPassword(passwordEncoder.encode(u.getPassword()));
			}
			Users user = ur.update(u);
			ResponseStructure<Users> response = new ResponseStructure<Users>();
			response.setStatuscode(HttpStatus.OK.value());
			response.setMessage("User Updated Successfully");
			response.setData(user);
			return new ResponseEntity<ResponseStructure<Users>>(response, HttpStatus.OK);
		} else {
			ResponseStructure<Users> response = new ResponseStructure<Users>();
			response.setStatuscode(HttpStatus.NOT_FOUND.value());
			response.setMessage("User not found");
			response.setData(null);
			return new ResponseEntity<ResponseStructure<Users>>(response, HttpStatus.NOT_FOUND);
		}
	}

	public ResponseEntity<ResponseStructure<String>> deleteUser(int id) {

		String user = ur.delete(id);
		if ("User Not Deleted".equals(user)) {
			ResponseStructure<String> response = new ResponseStructure<>();
			response.setStatuscode(HttpStatus.NOT_FOUND.value());
			response.setMessage("User not found");
			response.setData(user);
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}

		ResponseStructure<String> response = new ResponseStructure<String>();
		response.setStatuscode(HttpStatus.OK.value());
		response.setMessage("User Deleted Successfully");
		response.setData(user);
		return new ResponseEntity<ResponseStructure<String>>(response, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<List<Users>>> searchByName(String name) {
		List<Users> users = ur.searchByName(name);

		ResponseStructure<List<Users>> response = new ResponseStructure<>();
		response.setStatuscode(HttpStatus.OK.value());
		response.setMessage("Users Found");
		response.setData(users);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public Users login(String email, String password) {

		if (email == null || password == null) {
			return null;
		}

		Users user = ur.findByEmail(email);

		if (user == null || user.getPassword() == null) {
			return null;
		}

		try {
			boolean match = passwordEncoder.matches(password, user.getPassword());

			if (match) {
				return user;
			}
		} catch (Exception e) {
			return null;
		}

		return null;
	}

	private ResponseEntity<ResponseStructure<Users>> userResponse(HttpStatus status, String message, Users user) {
		ResponseStructure<Users> response = new ResponseStructure<>();
		response.setStatuscode(status.value());
		response.setMessage(message);
		response.setData(user);
		return new ResponseEntity<>(response, status);
	}

	/**
	 * Forgot Password endpoint.
	 * Accepts registered email and new password.
	 * Finds user by email, encodes new password with BCrypt, updates only the password field.
	 * Does NOT verify account ownership (intentional for unauthenticated reset flow).
	 * Does not return password or password hash in response.
	 */
	public ResponseEntity<ResponseStructure<Users>> forgotPassword(Users request) {
		String email = request.getProfile().getEmail();
		String newPassword = request.getPassword();

		if (newPassword == null || newPassword.isBlank()) {
			ResponseStructure<Users> response = new ResponseStructure<Users>();
			response.setStatuscode(HttpStatus.BAD_REQUEST.value());
			response.setMessage("Password is required");
			response.setData(null);
			return new ResponseEntity<ResponseStructure<Users>>(response, HttpStatus.BAD_REQUEST);
		}

		Users existing = ur.findByEmail(email);
		if (existing == null) {
			ResponseStructure<Users> response = new ResponseStructure<Users>();
			response.setStatuscode(HttpStatus.NOT_FOUND.value());
			response.setMessage("User not found with email: " + email);
			response.setData(null);
			return new ResponseEntity<ResponseStructure<Users>>(response, HttpStatus.NOT_FOUND);
		}

		// Update only the password field; preserve all other user information
		existing.setPassword(passwordEncoder.encode(newPassword));
		Users updated = ur.update(existing);

		ResponseStructure<Users> response = new ResponseStructure<Users>();
		response.setStatuscode(HttpStatus.OK.value());
		response.setMessage("Password updated successfully");
		response.setData(updated);
		return new ResponseEntity<ResponseStructure<Users>>(response, HttpStatus.OK);
	}
}