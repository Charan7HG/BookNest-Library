package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.pentagon.LibraryManagement.DAO.BookDAO;
import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.Service.BookService;
import com.pentagon.LibraryManagement.entity.Book;

/**
 * The legacy /bookbr endpoint must never write borrowing data again.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceBorrowTests {

	@Mock
	private BookDAO bd;

	@InjectMocks
	private BookService bookService;

	@Test
	void legacyBorrowEndpointIsGoneAndDoesNotTouchTheBook() {
		ResponseEntity<ResponseStructure<Book>> result = bookService.borrow(1, 1);

		assertEquals(HttpStatus.GONE, result.getStatusCode());
		assertEquals(410, result.getBody().getStatuscode());
		assertEquals("Direct borrowing is disabled. Submit a request via POST /borrow-requests",
				result.getBody().getMessage());
		assertNull(result.getBody().getData());
		verifyNoInteractions(bd);
	}

	@Test
	void legacyBorrowEndpointIgnoresNonExistentBookAndUser() {
		ResponseEntity<ResponseStructure<Book>> result = bookService.borrow(9999, 8888);

		assertEquals(HttpStatus.GONE, result.getStatusCode());
		verifyNoInteractions(bd);
	}
}
