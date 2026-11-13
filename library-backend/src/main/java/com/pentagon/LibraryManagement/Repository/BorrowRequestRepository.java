package com.pentagon.LibraryManagement.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pentagon.LibraryManagement.entity.BorrowRequest;
import com.pentagon.LibraryManagement.entity.BorrowRequestStatus;

import jakarta.persistence.LockModeType;

public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Integer> {

    boolean existsByMember_IdAndBook_IdAndStatus(int memberId, int bookId, BorrowRequestStatus status);

    List<BorrowRequest> findByMember_IdOrderByRequestedAtDesc(int memberId);

    List<BorrowRequest> findByStatusOrderByRequestedAtAsc(BorrowRequestStatus status);

    boolean existsByBook_IdAndStatus(int bookId, BorrowRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from BorrowRequest r where r.id = :id")
    Optional<BorrowRequest> findByIdForUpdate(@Param("id") int id);
}
