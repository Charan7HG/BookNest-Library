package com.pentagon.LibraryManagement.dto;

/**
 * Returned once to the ADMIN or LIBRARIAN who approves a password reset
 * request. The temporary password is shown so staff can pass it on securely.
 * It is never stored in plaintext, never returned from list endpoints, and
 * never logged.
 */
public class PasswordResetResponse {

    private int requestId;
    private int userId;
    private String email;
    private String temporaryPassword;
    private boolean passwordChangeRequired;

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired(boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
    }
}
