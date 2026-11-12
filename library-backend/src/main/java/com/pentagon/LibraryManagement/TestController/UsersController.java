package com.pentagon.LibraryManagement.TestController;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import com.pentagon.LibraryManagement.Service.UsersService;
import com.pentagon.LibraryManagement.entity.Users;

@RestController
public class UsersController {

	@Autowired
	private UsersService us;

	@PostMapping("/user")
	public ResponseEntity<ResponseStructure<Users>> addUser(@RequestBody Users u) {
		return us.addUser(u);
	}

	@GetMapping("/users")
	public ResponseEntity<ResponseStructure<List<Users>>> getAllUser() {
		return us.getAllUser();
	}

	@GetMapping("/user/{id}")
	public ResponseEntity<ResponseStructure<Users>> findUserById(@PathVariable int id) {
		return us.findUserById(id);
	}

	@GetMapping("/user/search")
	public ResponseEntity<ResponseStructure<List<Users>>> searchByName(@RequestParam String name) {
		return us.searchByName(name);
	}

	@PutMapping("/user/{id}")
	public ResponseEntity<ResponseStructure<Users>> updateUser(
			@PathVariable int id,
			@RequestBody Users u) {

		u.setId(id);

		return us.updateUser(u);
	}

	@DeleteMapping("/user/{id}")
	public ResponseEntity<ResponseStructure<String>> deleteUser(@PathVariable int id) {
		return us.deleteUser(id);
	}
}
