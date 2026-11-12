package com.pentagon.LibraryManagement.dto;

import jakarta.persistence.Column;

public class LoginRequest {
	 private String email;
	    @Column(name = "password")
	    private String password;

	    public String getEmail() {
	        return email;
	    }

	    public void setEmail(String email) {
	        this.email = email;
	    }

	    public String getPassword() {
	        return password;
	    }

	    public void setPassword(String password) {
	        this.password = password;
	    }
}
