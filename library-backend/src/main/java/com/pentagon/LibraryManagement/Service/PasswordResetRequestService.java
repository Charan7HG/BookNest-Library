package com.pentagon.LibraryManagement.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pentagon.LibraryManagement.DAO.PasswordResetRequestDAO;
import com.pentagon.LibraryManagement.DAO.UsersDAO;
import com.pentagon.LibraryManagement.dto.PasswordResetRequestResponse;
import com.pentagon.LibraryManagement.dto.PasswordResetResponse;
import com.pentagon.LibraryManagement.entity.PasswordResetRequest;
import com.pentagon.LibraryManagement.entity.PasswordResetRequestStatus;
import com.pentagon.LibraryManagement.entity.Role;
import com.pentagon.LibraryManagement.entity.Users;
import com.pentagon.LibraryManagement.exception.BorrowingException;

/**
 * Member-submitted password reset requests.
 *
 * Members never receive a password directly. Staff approve a pending request
 * and the approval generates the one-time temporary password.
 */
@Service
public class PasswordResetRequestService {

    public static final String GENERIC_SUBMISSION_MESSAGE =
            "If an eligible account exists, your password reset request has been submitted. "
                    + "Please contact your Librarian if you need assistance.";

    @Autowired
    private PasswordResetRequestDAO requestDAO;

    @Autowired
    private UsersDAO usersDAO;

    @Autowired
    private UsersService usersService;

    @Autowired
    private Clock clock;

    /**
     * Public submission. The response is always the same generic message so an
     * anonymous caller cannot discover which emails belong to members.
     *
     * The member row is locked while checking for an existing pending request,
     * so two simultaneous submissions cannot create duplicates. The partial
     * unique index in V5 is the final database-level guarantee.
     */
    @Transactional
    public void submitRequest(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        Users user = usersDAO.findByEmail(email.trim());
        if (user == null || user.getRole() != Role.MEMBER) {
            return;
        }

        Users locked = usersDAO.lockById(user.getId());
        if (locked == null || locked.getRole() != Role.MEMBER) {
            return;
        }

        if (requestDAO.existsPending(locked.getId())) {
            return;
        }

        requestDAO.saveAndFlush(new PasswordResetRequest(locked, now()));
    }

    @Transactional(readOnly = true)
    public List<PasswordResetRequestResponse> getPendingRequests() {
        requireStaff();
        return requestDAO.findPending().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PasswordResetRequestResponse> getAllRequests() {
        requireStaff();
        return requestDAO.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Approve a pending request: generate a temporary password, store only its
     * BCrypt hash, force a change at next login, and return the plaintext once.
     */
    @Transactional
    public PasswordResetResponse approveRequest(int requestId) {
        Users reviewer = requireStaff();

        PasswordResetRequest request = requestDAO.lockById(requestId)
                .orElseThrow(() -> new BorrowingException(HttpStatus.NOT_FOUND,
                        "Password reset request not found"));

        if (request.getStatus() != PasswordResetRequestStatus.PENDING) {
            throw new BorrowingException(HttpStatus.CONFLICT,
                    "Only pending password reset requests can be processed");
        }

        Users member = request.getUser();
        if (member == null || member.getRole() != Role.MEMBER) {
            throw new BorrowingException(HttpStatus.CONFLICT,
                    "The requesting account is not a MEMBER");
        }

        String temporaryPassword = usersService.applyTemporaryPassword(member);

        request.setStatus(PasswordResetRequestStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(now());
        requestDAO.save(request);

        PasswordResetResponse response = new PasswordResetResponse();
        response.setRequestId(request.getId());
        response.setUserId(member.getId());
        response.setEmail(member.getProfile() != null ? member.getProfile().getEmail() : null);
        response.setTemporaryPassword(temporaryPassword);
        response.setPasswordChangeRequired(true);
        return response;
    }

    /**
     * Reject a pending request. The member's password is never touched.
     */
    @Transactional
    public PasswordResetRequestResponse rejectRequest(int requestId) {
        Users reviewer = requireStaff();

        PasswordResetRequest request = requestDAO.lockById(requestId)
                .orElseThrow(() -> new BorrowingException(HttpStatus.NOT_FOUND,
                        "Password reset request not found"));

        if (request.getStatus() != PasswordResetRequestStatus.PENDING) {
            throw new BorrowingException(HttpStatus.CONFLICT,
                    "Only pending password reset requests can be processed");
        }

        request.setStatus(PasswordResetRequestStatus.REJECTED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(now());
        requestDAO.save(request);

        return toResponse(request);
    }

    private Users requireStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BorrowingException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            throw new BorrowingException(HttpStatus.FORBIDDEN, "Authenticated email is required");
        }

        Users user = usersDAO.findByEmail(email);
        if (user == null) {
            throw new BorrowingException(HttpStatus.UNAUTHORIZED, "Authenticated user not found");
        }
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.LIBRARIAN) {
            throw new BorrowingException(HttpStatus.FORBIDDEN, "ADMIN or LIBRARIAN access required");
        }
        return user;
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private PasswordResetRequestResponse toResponse(PasswordResetRequest request) {
        PasswordResetRequestResponse response = new PasswordResetRequestResponse();
        response.setRequestId(request.getId());
        response.setStatus(request.getStatus().name());
        response.setRequestedAt(request.getRequestedAt());

        Users member = request.getUser();
        if (member != null) {
            response.setMemberId(member.getId());
            response.setMemberName(member.getName());
            if (member.getProfile() != null) {
                response.setMemberEmail(member.getProfile().getEmail());
            }
        }

        Users reviewer = request.getReviewedBy();
        if (reviewer != null) {
            response.setReviewedById(reviewer.getId());
            response.setReviewedByName(reviewer.getName());
        }
        response.setReviewedAt(request.getReviewedAt());
        return response;
    }
}
