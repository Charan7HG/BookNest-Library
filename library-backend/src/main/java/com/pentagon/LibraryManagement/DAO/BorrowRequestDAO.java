package com.pentagon.LibraryManagement.DAO;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.pentagon.LibraryManagement.Repository.BorrowRequestRepository;
import com.pentagon.LibraryManagement.entity.BorrowRequest;
import com.pentagon.LibraryManagement.entity.BorrowRequestStatus;

@Repository
public class BorrowRequestDAO {

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    public boolean existsPending(int memberId, int bookId) {
        return borrowRequestRepository.existsByMember_IdAndBook_IdAndStatus(
                memberId, bookId, BorrowRequestStatus.PENDING);
    }

    public boolean existsPendingForBook(int bookId) {
        return borrowRequestRepository.existsByBook_IdAndStatus(bookId, BorrowRequestStatus.PENDING);
    }

    public List<BorrowRequest> findByMember(int memberId) {
        return borrowRequestRepository.findByMember_IdOrderByRequestedAtDesc(memberId);
    }

    public List<BorrowRequest> findPending() {
        return borrowRequestRepository.findByStatusOrderByRequestedAtAsc(BorrowRequestStatus.PENDING);
    }

    public Optional<BorrowRequest> lockById(int requestId) {
        return borrowRequestRepository.findByIdForUpdate(requestId);
    }

    public BorrowRequest saveAndFlush(BorrowRequest request) {
        return borrowRequestRepository.saveAndFlush(request);
    }

    public BorrowRequest save(BorrowRequest request) {
        return borrowRequestRepository.save(request);
    }
}
