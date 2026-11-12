package com.pentagon.LibraryManagement.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pentagon.LibraryManagement.entity.Loan;
import com.pentagon.LibraryManagement.entity.LoanStatus;

import jakarta.persistence.LockModeType;

public interface LoanRepository extends JpaRepository<Loan, Integer> {

    long countByMember_IdAndStatus(int memberId, LoanStatus status);

    boolean existsByMember_IdAndCopy_Book_IdAndStatus(int memberId, int bookId, LoanStatus status);

    List<Loan> findByStatusOrderByIdAsc(LoanStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> findByIdForUpdate(@Param("id") int id);
}
