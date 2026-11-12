package com.pentagon.LibraryManagement.dto;

/**
 * Body for the public password-reset request form. Only an email is accepted;
 * the client can never supply a user id.
 */
public class PasswordResetRequestCreateRequest {

    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
