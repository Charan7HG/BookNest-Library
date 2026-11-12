package com.pentagon.LibraryManagement.dto;

/**
 * Body for the authenticated change-password endpoint. The user is always
 * taken from the JWT, never from a request parameter.
 */
public class ChangePasswordRequest {

    private String currentPassword;
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
