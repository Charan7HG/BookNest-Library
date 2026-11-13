package com.pentagon.LibraryManagement.DAO;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.pentagon.LibraryManagement.Repository.LoanRepository;
import com.pentagon.LibraryManagement.entity.Loan;
import com.pentagon.LibraryManagement.entity.LoanStatus;

@Repository
public class LoanDAO {

    @Autowired
    private LoanRepository loanRepository;

    public long countActiveByMember(int memberId) {
        return loanRepository.countByMember_IdAndStatus(memberId, LoanStatus.ACTIVE);
    }

    public boolean existsActiveForMemberAndBook(int memberId, int bookId) {
        return loanRepository.existsByMember_IdAndCopy_Book_IdAndStatus(memberId, bookId, LoanStatus.ACTIVE);
    }

    public boolean existsAnyForBook(int bookId) {
        return loanRepository.existsByCopy_Book_Id(bookId);
    }

    public List<Loan> findActiveLoans() {
        return loanRepository.findByStatusOrderByIdAsc(LoanStatus.ACTIVE);
    }

    public Optional<Loan> lockById(int loanId) {
        return loanRepository.findByIdForUpdate(loanId);
    }

    public Loan saveAndFlush(Loan loan) {
        return loanRepository.saveAndFlush(loan);
    }

    public Loan save(Loan loan) {
        return loanRepository.save(loan);
    }
}
