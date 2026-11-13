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
 * Staff-assisted password reset requests.
 *
 * MEMBER and LIBRARIAN accounts may submit a request. ADMIN accounts never use
 * this queue and change their own password while authenticated instead.
 *
 * Members and librarians never receive a password directly. An authorised
 * staff member approves a pending request and the approval generates the
 * one-time temporary password.
 */
@Service
public class PasswordResetRequestService {

    public static final String GENERIC_SUBMISSION_MESSAGE =
            "If an eligible account exists, your password reset request has been submitted. "
                    + "Please contact library staff if you need assistance.";

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
     * anonymous caller cannot discover which emails belong to eligible accounts.
     *
     * The account row is locked while checking for an existing pending request,
     * so two simultaneous submissions cannot create duplicates. The partial
     * unique index in V5 is the final database-level guarantee.
     *
     * ADMIN accounts are deliberately excluded: they must use the
     * authenticated self-service Change Password page.
     */
    @Transactional
    public void submitRequest(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        Users user = usersDAO.findByEmail(email.trim());
        if (user == null || !canSubmitRequest(user.getRole())) {
            return;
        }

        Users locked = usersDAO.lockById(user.getId());
        if (locked == null || !canSubmitRequest(locked.getRole())) {
            return;
        }

        if (requestDAO.existsPending(locked.getId())) {
            return;
        }

        requestDAO.saveAndFlush(new PasswordResetRequest(locked, now()));
    }

    @Transactional(readOnly = true)
    public List<PasswordResetRequestResponse> getPendingRequests() {
        Users reviewer = requireStaff();
        return requestDAO.findPending().stream()
                .filter(request -> canView(reviewer, request))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PasswordResetRequestResponse> getAllRequests() {
        Users reviewer = requireStaff();
        return requestDAO.findAll().stream()
                .filter(request -> canView(reviewer, request))
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

        Users target = request.getUser();
        requireCanProcess(reviewer, target);

        String temporaryPassword = usersService.applyTemporaryPassword(target);

        request.setStatus(PasswordResetRequestStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(now());
        requestDAO.save(request);

        PasswordResetResponse response = new PasswordResetResponse();
        response.setRequestId(request.getId());
        response.setUserId(target.getId());
        response.setEmail(target.getProfile() != null ? target.getProfile().getEmail() : null);
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

        requireCanProcess(reviewer, request.getUser());

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

    private boolean canSubmitRequest(Role role) {
        return role == Role.MEMBER || role == Role.LIBRARIAN;
    }

    /**
     * ADMIN sees the whole queue. A LIBRARIAN only sees MEMBER requests, so a
     * librarian request can never be approved or rejected by another librarian.
     */
    private boolean canView(Users reviewer, PasswordResetRequest request) {
        Users target = request.getUser();
        if (target == null) {
            return false;
        }
        if (reviewer.getRole() == Role.ADMIN) {
            return true;
        }
        return reviewer.getRole() == Role.LIBRARIAN && target.getRole() == Role.MEMBER;
    }

    /**
     * Applies the approval matrix and blocks self-service through the queue.
     *
     * MEMBER requests  -> ADMIN or LIBRARIAN
     * LIBRARIAN request -> ADMIN only
     * ADMIN request     -> never exists in this queue
     */
    private void requireCanProcess(Users reviewer, Users target) {
        if (target == null) {
            throw new BorrowingException(HttpStatus.CONFLICT,
                    "The password reset request has no account");
        }

        if (reviewer.getId() == target.getId()) {
            throw new BorrowingException(HttpStatus.FORBIDDEN,
                    "Staff cannot reset their own password through the request queue");
        }

        boolean allowed;
        if (reviewer.getRole() == Role.ADMIN) {
            allowed = target.getRole() == Role.MEMBER || target.getRole() == Role.LIBRARIAN;
        } else if (reviewer.getRole() == Role.LIBRARIAN) {
            allowed = target.getRole() == Role.MEMBER;
        } else {
            allowed = false;
        }

        if (!allowed) {
            throw new BorrowingException(HttpStatus.FORBIDDEN,
                    "You are not allowed to process this password reset request");
        }
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
            response.setAccountRole(member.getRole() != null ? member.getRole().name() : null);
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
