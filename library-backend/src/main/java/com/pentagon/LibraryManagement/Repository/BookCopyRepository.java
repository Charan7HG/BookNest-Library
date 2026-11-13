package com.pentagon.LibraryManagement.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pentagon.LibraryManagement.entity.BookCopy;
import com.pentagon.LibraryManagement.entity.BookCopyStatus;

import jakarta.persistence.LockModeType;

public interface BookCopyRepository extends JpaRepository<BookCopy, Integer> {

    boolean existsByBook_IdAndStatus(int bookId, BookCopyStatus status);

    long countByBook_IdAndStatus(int bookId, BookCopyStatus status);

    long countByBook_Id(int bookId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BookCopy c where c.book.id = :bookId")
    void deleteByBookId(@Param("bookId") int bookId);

    /**
     * Locks AVAILABLE copies of a book with SELECT ... FOR UPDATE and returns them
     * in a deterministic order. A concurrent approval waits for these row locks;
     * once the first transaction commits, the copy it issued no longer satisfies
     * status = AVAILABLE and is skipped.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopy c where c.book.id = :bookId and c.status = :status order by c.id")
    List<BookCopy> findAvailableForUpdate(@Param("bookId") int bookId,
            @Param("status") BookCopyStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopy c where c.id = :id")
    Optional<BookCopy> findByIdForUpdate(@Param("id") int id);
}
