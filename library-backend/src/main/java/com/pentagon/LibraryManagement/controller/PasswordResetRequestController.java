package com.pentagon.LibraryManagement.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.Service.PasswordResetRequestService;
import com.pentagon.LibraryManagement.dto.PasswordResetRequestCreateRequest;
import com.pentagon.LibraryManagement.dto.PasswordResetRequestResponse;
import com.pentagon.LibraryManagement.dto.PasswordResetResponse;

/**
 * In-application password recovery requests.
 *
 * Members submit a request anonymously. ADMIN and LIBRARIAN accounts list,
 * approve, or reject pending requests. No email, OTP, or external service is
 * involved.
 */
@RestController
public class PasswordResetRequestController {

    @Autowired
    private PasswordResetRequestService passwordResetRequestService;

    // Public: a member asks for help. The response is always generic.
    @PostMapping("/password-reset-requests")
    public ResponseEntity<ResponseStructure<Void>> submitRequest(
            @RequestBody(required = false) PasswordResetRequestCreateRequest body) {

        passwordResetRequestService.submitRequest(body == null ? null : body.getEmail());
        return response(HttpStatus.OK, PasswordResetRequestService.GENERIC_SUBMISSION_MESSAGE, null);
    }

    // ADMIN / LIBRARIAN: pending queue.
    @GetMapping("/password-reset-requests/pending")
    public ResponseEntity<ResponseStructure<List<PasswordResetRequestResponse>>> getPendingRequests() {

        List<PasswordResetRequestResponse> data = passwordResetRequestService.getPendingRequests();
        return response(HttpStatus.OK, "Pending password reset requests fetched", data);
    }

    // ADMIN / LIBRARIAN: complete history, newest first.
    @GetMapping("/password-reset-requests")
    public ResponseEntity<ResponseStructure<List<PasswordResetRequestResponse>>> getAllRequests() {

        List<PasswordResetRequestResponse> data = passwordResetRequestService.getAllRequests();
        return response(HttpStatus.OK, "Password reset requests fetched", data);
    }

    // ADMIN / LIBRARIAN: approve and receive the temporary password exactly once.
    @PostMapping("/password-reset-requests/{id}/approve")
    public ResponseEntity<ResponseStructure<PasswordResetResponse>> approveRequest(@PathVariable int id) {

        PasswordResetResponse data = passwordResetRequestService.approveRequest(id);
        return response(HttpStatus.OK,
                "Password reset request approved. Share the temporary password securely; "
                        + "it is shown only once.",
                data);
    }

    // ADMIN / LIBRARIAN: reject without changing the member's password.
    @PostMapping("/password-reset-requests/{id}/reject")
    public ResponseEntity<ResponseStructure<PasswordResetRequestResponse>> rejectRequest(@PathVariable int id) {

        PasswordResetRequestResponse data = passwordResetRequestService.rejectRequest(id);
        return response(HttpStatus.OK, "Password reset request rejected", data);
    }

    private <T> ResponseEntity<ResponseStructure<T>> response(HttpStatus status, String message, T data) {
        ResponseStructure<T> body = new ResponseStructure<T>();
        body.setStatuscode(status.value());
        body.setMessage(message);
        body.setData(data);
        return new ResponseEntity<ResponseStructure<T>>(body, status);
    }
}
