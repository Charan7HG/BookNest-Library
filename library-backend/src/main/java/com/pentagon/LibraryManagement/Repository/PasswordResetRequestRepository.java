package com.pentagon.LibraryManagement.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pentagon.LibraryManagement.entity.PasswordResetRequest;
import com.pentagon.LibraryManagement.entity.PasswordResetRequestStatus;

import jakarta.persistence.LockModeType;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Integer> {

    boolean existsByUser_IdAndStatus(int userId, PasswordResetRequestStatus status);

    List<PasswordResetRequest> findByStatusOrderByRequestedAtAsc(PasswordResetRequestStatus status);

    List<PasswordResetRequest> findAllByOrderByRequestedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PasswordResetRequest r where r.id = :id")
    Optional<PasswordResetRequest> findByIdForUpdate(@Param("id") int id);
}
