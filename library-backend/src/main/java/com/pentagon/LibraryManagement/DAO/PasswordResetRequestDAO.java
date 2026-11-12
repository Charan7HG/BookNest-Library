package com.pentagon.LibraryManagement.DAO;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.pentagon.LibraryManagement.Repository.PasswordResetRequestRepository;
import com.pentagon.LibraryManagement.entity.PasswordResetRequest;
import com.pentagon.LibraryManagement.entity.PasswordResetRequestStatus;

@Repository
public class PasswordResetRequestDAO {

    @Autowired
    private PasswordResetRequestRepository repository;

    public boolean existsPending(int userId) {
        return repository.existsByUser_IdAndStatus(userId, PasswordResetRequestStatus.PENDING);
    }

    public List<PasswordResetRequest> findPending() {
        return repository.findByStatusOrderByRequestedAtAsc(PasswordResetRequestStatus.PENDING);
    }

    public List<PasswordResetRequest> findAll() {
        return repository.findAllByOrderByRequestedAtDesc();
    }

    public Optional<PasswordResetRequest> lockById(int requestId) {
        return repository.findByIdForUpdate(requestId);
    }

    public PasswordResetRequest saveAndFlush(PasswordResetRequest request) {
        return repository.saveAndFlush(request);
    }

    public PasswordResetRequest save(PasswordResetRequest request) {
        return repository.save(request);
    }
}
