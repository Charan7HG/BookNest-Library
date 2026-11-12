package com.pentagon.LibraryManagement.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;

@RestControllerAdvice
public class BorrowingExceptionHandler {

    @ExceptionHandler(BorrowingException.class)
    public ResponseEntity<ResponseStructure<Void>> handleBorrowingException(BorrowingException exception) {
        ResponseStructure<Void> body = new ResponseStructure<>();
        body.setStatuscode(exception.getStatus().value());
        body.setMessage(exception.getMessage());
        body.setData(null);
        return new ResponseEntity<>(body, exception.getStatus());
    }
}
