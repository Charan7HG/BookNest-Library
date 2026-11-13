package com.pentagon.LibraryManagement.Service;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.pentagon.LibraryManagement.DAO.UsersDAO;
import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.dto.ChangePasswordRequest;
import com.pentagon.LibraryManagement.entity.Role;
import com.pentagon.LibraryManagement.entity.Users;

@Service
public class UsersService {

	// Cryptographically secure generator for staff-issued temporary passwords.
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	// 16 characters from an unambiguous alphabet gives a strong one-time password
	// while staying easy to read aloud or type.
	private static final char[] TEMP_PASSWORD_ALPHABET =
			"ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*".toCharArray();
	private static final int TEMP_PASSWORD_LENGTH = 16;
	private static final int MIN_PASSWORD_LENGTH = 8;

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
		u.setPasswordChangeRequired(false);
		if (u.getRole() == null) {
			u.setRole(Role.MEMBER);
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
			// The forced-change flag is only changed by the reset/change-password flows.
			u.setPasswordChangeRequired(existing.isPasswordChangeRequired());
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
	 * Applies a fresh staff-issued temporary password to a member.
	 *
	 * This method is only called from the approved password-reset request
	 * workflow. There is deliberately no HTTP endpoint that calls it directly,
	 * so a password can never be reset without an approved request.
	 *
	 * The plaintext value is returned to the caller for the one-time response.
	 * Only the BCrypt hash is persisted, and nothing is logged.
	 */
	public String applyTemporaryPassword(Users target) {
		String temporaryPassword = generateTemporaryPassword();
		target.setPassword(passwordEncoder.encode(temporaryPassword));
		target.setPasswordChangeRequired(true);
		ur.update(target);
		return temporaryPassword;
	}

	/**
	 * Authenticated password change for the current user. The user is taken from
	 * the JWT, so one account can never change another account's password here.
	 * Clearing the forced-change flag restores normal access.
	 */
	public ResponseEntity<ResponseStructure<Void>> changePassword(String email, ChangePasswordRequest request) {
		if (request == null || request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()
				|| request.getNewPassword() == null || request.getNewPassword().isBlank()) {
			return passwordChangeError(HttpStatus.BAD_REQUEST,
					"Current password and new password are required");
		}

		if (request.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
			return passwordChangeError(HttpStatus.BAD_REQUEST,
					"New password must be at least " + MIN_PASSWORD_LENGTH + " characters");
		}

		Users user = ur.findByEmail(email);
		if (user == null) {
			return passwordChangeError(HttpStatus.NOT_FOUND, "User not found");
		}
		if (user.getPassword() == null
				|| !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
			return passwordChangeError(HttpStatus.BAD_REQUEST, "Current password is incorrect");
		}
		if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
			return passwordChangeError(HttpStatus.BAD_REQUEST,
					"New password must be different from the current password");
		}

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		user.setPasswordChangeRequired(false);
		ur.update(user);

		ResponseStructure<Void> response = new ResponseStructure<>();
		response.setStatuscode(HttpStatus.OK.value());
		response.setMessage("Password changed successfully");
		response.setData(null);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	private String generateTemporaryPassword() {
		StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);
		for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
			password.append(TEMP_PASSWORD_ALPHABET[SECURE_RANDOM.nextInt(TEMP_PASSWORD_ALPHABET.length)]);
		}
		return password.toString();
	}

	private ResponseEntity<ResponseStructure<Void>> passwordChangeError(HttpStatus status, String message) {
		ResponseStructure<Void> response = new ResponseStructure<>();
		response.setStatuscode(status.value());
		response.setMessage(message);
		response.setData(null);
		return new ResponseEntity<>(response, status);
	}
}
