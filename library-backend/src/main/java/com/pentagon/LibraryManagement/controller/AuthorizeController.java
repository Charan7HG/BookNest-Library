package com.pentagon.LibraryManagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.pentagon.LibraryManagement.security.JwtUtil;
import com.pentagon.LibraryManagement.dto.LoginResponse;

import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.Service.UsersService;
import com.pentagon.LibraryManagement.dto.LoginRequest;
import com.pentagon.LibraryManagement.entity.Users;
@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class AuthorizeController {
	@Autowired
    private UsersService usersService;

	@Autowired
	private JwtUtil jwtUtil;

	@PostMapping("/login")
	public ResponseEntity<ResponseStructure<LoginResponse>> login(
	        @RequestBody LoginRequest request) {
	    Users user = usersService.login(
	            request.getEmail(),
	            request.getPassword());
        if (user != null) {

            String token =
                    jwtUtil.generateToken(user.getProfile().getEmail(), user.getRole().name());

            LoginResponse loginResponse = new LoginResponse();

            loginResponse.setToken(token);
            loginResponse.setTokenType("Bearer");
            loginResponse.setUserId(user.getId());
            loginResponse.setName(user.getName());
            loginResponse.setEmail(user.getProfile().getEmail());
            loginResponse.setRole(user.getRole().name());

            LoginResponse.User safeUser = new LoginResponse.User();
            safeUser.setId(user.getId());
            safeUser.setName(user.getName());
            safeUser.setEmail(user.getProfile().getEmail());
            safeUser.setRole(user.getRole().name());
            loginResponse.setUser(safeUser);

            ResponseStructure<LoginResponse> responser=  new ResponseStructure<>();

            responser.setStatuscode(HttpStatus.OK.value());
            responser.setMessage("Login Successful");
            responser.setData(loginResponse);

            return new ResponseEntity<>(responser, HttpStatus.OK);

        } else {

        	ResponseStructure<LoginResponse> responsef =new ResponseStructure<>();

        	responsef.setStatuscode(HttpStatus.UNAUTHORIZED.value());
        	responsef.setMessage("Invalid Email or Password");
        	responsef.setData(null);

        	return new ResponseEntity<>(responsef, HttpStatus.UNAUTHORIZED);
        }
    }
}
