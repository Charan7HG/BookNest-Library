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
import com.pentagon.LibraryManagement.Service.BorrowRequestService;
import com.pentagon.LibraryManagement.dto.BorrowRequestCreateRequest;
import com.pentagon.LibraryManagement.dto.BorrowRequestResponse;
import com.pentagon.LibraryManagement.dto.LoanResponse;

/**
 * Member borrowing workflow.
 *
 * The authenticated identity is resolved inside {@link BorrowRequestService}
 * from the security context. No client-supplied member id is accepted.
 */
@RestController
public class BorrowRequestController {

    @Autowired
    private BorrowRequestService borrowRequestService;

    // MEMBER: submit a request for a book.
    @PostMapping("/borrow-requests")
    public ResponseEntity<ResponseStructure<BorrowRequestResponse>> createRequest(
            @RequestBody BorrowRequestCreateRequest body) {

        BorrowRequestResponse data = borrowRequestService.createRequest(body);
        return response(HttpStatus.CREATED, "Borrow request submitted", data);
    }

    // MEMBER: list own request history.
    @GetMapping("/borrow-requests/my")
    public ResponseEntity<ResponseStructure<List<BorrowRequestResponse>>> getMyRequests() {

        List<BorrowRequestResponse> data = borrowRequestService.getMyRequests();
        return response(HttpStatus.OK, "Borrow requests fetched", data);
    }

    // ADMIN / LIBRARIAN: list pending requests.
    @GetMapping("/borrow-requests/pending")
    public ResponseEntity<ResponseStructure<List<BorrowRequestResponse>>> getPendingRequests() {

        List<BorrowRequestResponse> data = borrowRequestService.getPendingRequests();
        return response(HttpStatus.OK, "Pending borrow requests fetched", data);
    }

    // ADMIN / LIBRARIAN: approve a pending request and issue a copy.
    @PostMapping("/borrow-requests/{id}/approve")
    public ResponseEntity<ResponseStructure<BorrowRequestResponse>> approveRequest(@PathVariable int id) {

        BorrowRequestResponse data = borrowRequestService.approveRequest(id);
        return response(HttpStatus.OK, "Borrow request approved", data);
    }

    // ADMIN / LIBRARIAN: reject a pending request.
    @PostMapping("/borrow-requests/{id}/reject")
    public ResponseEntity<ResponseStructure<BorrowRequestResponse>> rejectRequest(@PathVariable int id) {

        BorrowRequestResponse data = borrowRequestService.rejectRequest(id);
        return response(HttpStatus.OK, "Borrow request rejected", data);
    }

    // ADMIN / LIBRARIAN: list active loans.
    @GetMapping("/loans/active")
    public ResponseEntity<ResponseStructure<List<LoanResponse>>> getActiveLoans() {

        List<LoanResponse> data = borrowRequestService.getActiveLoans();
        return response(HttpStatus.OK, "Active loans fetched", data);
    }

    // ADMIN / LIBRARIAN: record a return.
    @PostMapping("/loans/{id}/return")
    public ResponseEntity<ResponseStructure<LoanResponse>> returnLoan(@PathVariable int id) {

        LoanResponse data = borrowRequestService.returnLoan(id);
        return response(HttpStatus.OK, "Book returned", data);
    }

    private <T> ResponseEntity<ResponseStructure<T>> response(HttpStatus status, String message, T data) {
        ResponseStructure<T> body = new ResponseStructure<T>();
        body.setStatuscode(status.value());
        body.setMessage(message);
        body.setData(data);
        return new ResponseEntity<ResponseStructure<T>>(body, status);
    }
}
