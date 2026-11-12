package com.pentagon.LibraryManagement.DAO;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.pentagon.LibraryManagement.Repository.BookCopyRepository;
import com.pentagon.LibraryManagement.entity.BookCopy;
import com.pentagon.LibraryManagement.entity.BookCopyStatus;

@Repository
public class BookCopyDAO {

    @Autowired
    private BookCopyRepository bookCopyRepository;

    public boolean existsAvailableForBook(int bookId) {
        return bookCopyRepository.existsByBook_IdAndStatus(bookId, BookCopyStatus.AVAILABLE);
    }

    public Optional<BookCopy> lockFirstAvailableForBook(int bookId) {
        return bookCopyRepository.findAvailableForUpdate(bookId, BookCopyStatus.AVAILABLE)
                .stream()
                .findFirst();
    }

    public Optional<BookCopy> lockById(int copyId) {
        return bookCopyRepository.findByIdForUpdate(copyId);
    }

    public BookCopy save(BookCopy copy) {
        return bookCopyRepository.save(copy);
    }
}
