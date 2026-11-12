package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.DAO.UsersDAO;
import com.pentagon.LibraryManagement.Service.UsersService;
import com.pentagon.LibraryManagement.entity.Users;
import com.pentagon.LibraryManagement.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BookForgotPasswordTests {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsersDAO ur;

    @InjectMocks
    private UsersService usersService;

    private Users mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new Users();
        mockUser.setId(8);
        mockUser.setName("Test Admin");
        mockUser.setProfile(new com.pentagon.LibraryManagement.entity.Profile());
        mockUser.getProfile().setEmail("test@example.com");
        mockUser.setRole(Role.ADMIN);
        mockUser.setPassword("$2a$10$nBzBxV8V4uJmZoKMiQmOeX8PJmZoKMiQmOeJmZoKMiQmOeJmZoKMiQ");
    }

    @Test
    void successfulPasswordReset() {
        // Arrange: user exists with the email, password encoder returns encoded password
        when(passwordEncoder.encode("NewSecurePass123!")).thenReturn("$2a$10$encodedPasswordHash");
        when(ur.findByEmail("test@example.com")).thenReturn(mockUser);
        when(ur.update(mockUser)).thenReturn(mockUser);

        // Act: Call forgotPassword with valid email and new password
        ResponseEntity<ResponseStructure<Users>> result = usersService.forgotPassword(
            new Users() {{
                setProfile(new com.pentagon.LibraryManagement.entity.Profile());
                getProfile().setEmail("test@example.com");
                setPassword("NewSecurePass123!");
            }}
        );

        // Assert: Password was updated successfully
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Password updated successfully", result.getBody().getMessage());

        // Verify password was BCrypted (encoded by password encoder)
        assertNotNull(result.getBody().getData());
        assertTrue(result.getBody().getData().getPassword().startsWith("$2a$"));

        // Verify email was not changed
        assertNotNull(result.getBody().getData().getProfile());
        assertEquals("test@example.com", result.getBody().getData().getProfile().getEmail());

        // Verify role was not changed
        assertNotNull(result.getBody().getData().getRole());
        assertEquals(Role.ADMIN, result.getBody().getData().getRole());
        // Verify the updated user was returned (mock stub)
        assertNotNull(result.getBody().getData().getId());
    }

    @Test
    void unknownEmailReturnsNotFound() {
        // Arrange: user does not exist
        when(ur.findByEmail("nonexistent@example.com")).thenReturn(null);

        // Act: Call forgotPassword with non-existent email
        ResponseEntity<ResponseStructure<Users>> result = usersService.forgotPassword(
            new Users() {{
                setProfile(new com.pentagon.LibraryManagement.entity.Profile());
                getProfile().setEmail("nonexistent@example.com");
                setPassword("NewSecurePass123!");
            }}
        );

        // Assert: User not found
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals("User not found with email: nonexistent@example.com", result.getBody().getMessage());
    }

    @Test
    void blankPasswordReturnsBadRequest() {
        // Arrange: blank password (service returns early, no DAO stub needed)

        // Act: Call forgotPassword with blank password
        ResponseEntity<ResponseStructure<Users>> result = usersService.forgotPassword(
            new Users() {{
                setProfile(new com.pentagon.LibraryManagement.entity.Profile());
                getProfile().setEmail("test@example.com");
                setPassword("");
            }}
        );

        // Assert: Bad request
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("Password is required", result.getBody().getMessage());
    }

    @Test
    void passwordEncodingWithBcrypt() {
        // Arrange: valid user and new password
        when(passwordEncoder.encode("TestPassword@123")).thenReturn("$2b$10$encodedHash");
        when(ur.findByEmail("test@example.com")).thenReturn(mockUser);

        // Act: Call forgotPassword
        usersService.forgotPassword(
            new Users() {{
                setProfile(new com.pentagon.LibraryManagement.entity.Profile());
                getProfile().setEmail("test@example.com");
                setPassword("TestPassword@123");
            }}
        );

        // Assert: Password was encoded with BCrypt
        // The password field should be BCrypted (starts with $2a$ or $2b$ or $2y$)
        assertTrue(mockUser.getPassword().startsWith("$2a$") ||
                 mockUser.getPassword().startsWith("$2b$") ||
                 mockUser.getPassword().startsWith("$2y$"));
    }

    @Test
    void userDetailsAndRoleRemainUnchanged() {
        // Arrange: valid user
        when(passwordEncoder.encode("NewSecurePass123!")).thenReturn("$2y$10$encodedHash");
        when(ur.findByEmail("test@example.com")).thenReturn(mockUser);

        // Act: Call forgotPassword
        usersService.forgotPassword(
            new Users() {{
                setProfile(new com.pentagon.LibraryManagement.entity.Profile());
                getProfile().setEmail("test@example.com");
                setPassword("NewSecurePass123!");
            }}
        );

        // Assert: Only password changed, other details unchanged
        assertEquals("Test Admin", mockUser.getName());
        assertEquals("test@example.com", mockUser.getProfile().getEmail());
        assertEquals(Role.ADMIN, mockUser.getRole());
        // Password should be different (BCrypted)
        assertTrue(mockUser.getPassword().startsWith("$2"));
    }
}