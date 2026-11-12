package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pentagon.LibraryManagement.DAO.BookDAO;
import com.pentagon.LibraryManagement.Repository.BookRepository;
import com.pentagon.LibraryManagement.entity.Book;
import com.pentagon.LibraryManagement.entity.Users;

/**
 * book.user_id is legacy data. Catalog writes must never overwrite it.
 */
@ExtendWith(MockitoExtension.class)
class BookDAOLegacyTests {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookDAO bookDAO;

    @Test
    void addBookIgnoresAClientSuppliedBorrower() {
        Book incoming = new Book();
        incoming.setName("New");
        incoming.setPrice(5.0);
        incoming.setUsers(new Users());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book saved = bookDAO.addBook(incoming);

        assertNull(saved.getUsers());
    }

    @Test
    void updateBookPreservesTheExistingLegacyBorrower() {
        Users legacyBorrower = new Users();
        legacyBorrower.setId(13);

        Book existing = new Book();
        existing.setId(46);
        existing.setName("Old");
        existing.setPrice(1.0);
        existing.setUsers(legacyBorrower);

        Book update = new Book();
        update.setName("New name");
        update.setPrice(2.0);
        update.setUsers(null);

        when(bookRepository.findById(46)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book saved = bookDAO.updateBook(46, update);

        assertEquals("New name", saved.getName());
        assertEquals(2.0, saved.getPrice());
        assertEquals(legacyBorrower, saved.getUsers());
        verify(bookRepository).save(existing);
    }

    @Test
    void readingABookDoesNotClearItsLegacyBorrower() {
        Users legacyBorrower = new Users();
        legacyBorrower.setId(13);
        Book existing = new Book();
        existing.setId(46);
        existing.setUsers(legacyBorrower);

        when(bookRepository.findById(46)).thenReturn(Optional.of(existing));

        Book found = bookDAO.findBookById(46);

        assertEquals(legacyBorrower, found.getUsers());
    }
}
