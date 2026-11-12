package com.pentagon.LibraryManagement.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pentagon.LibraryManagement.DAO.BookCopyDAO;
import com.pentagon.LibraryManagement.DAO.BookDAO;
import com.pentagon.LibraryManagement.DAO.BorrowRequestDAO;
import com.pentagon.LibraryManagement.DAO.LoanDAO;
import com.pentagon.LibraryManagement.DAO.UsersDAO;
import com.pentagon.LibraryManagement.config.AppConfig;
import com.pentagon.LibraryManagement.dto.BorrowRequestCreateRequest;
import com.pentagon.LibraryManagement.dto.BorrowRequestResponse;
import com.pentagon.LibraryManagement.dto.LoanResponse;
import com.pentagon.LibraryManagement.entity.Book;
import com.pentagon.LibraryManagement.entity.BookCopy;
import com.pentagon.LibraryManagement.entity.BookCopyStatus;
import com.pentagon.LibraryManagement.entity.BorrowRequest;
import com.pentagon.LibraryManagement.entity.BorrowRequestStatus;
import com.pentagon.LibraryManagement.entity.Loan;
import com.pentagon.LibraryManagement.entity.LoanStatus;
import com.pentagon.LibraryManagement.entity.Role;
import com.pentagon.LibraryManagement.entity.Users;
import com.pentagon.LibraryManagement.exception.BorrowingException;

@Service
public class BorrowRequestService {

    @Autowired
    private BorrowRequestDAO borrowRequestDAO;

    @Autowired
    private BookCopyDAO bookCopyDAO;

    @Autowired
    private LoanDAO loanDAO;

    @Autowired
    private BookDAO bookDAO;

    @Autowired
    private UsersDAO usersDAO;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private Clock clock;

    @Transactional
    public BorrowRequestResponse createRequest(BorrowRequestCreateRequest body) {
        Users member = requireMember();

        if (body == null || body.getBookId() == null) {
            throw new BorrowingException(HttpStatus.BAD_REQUEST, "Book id is required");
        }

        int bookId = body.getBookId();
        Book book = bookDAO.getById(bookId);
        if (book == null) {
            throw new BorrowingException(HttpStatus.NOT_FOUND, "Book not found");
        }

        if (borrowRequestDAO.existsPending(member.getId(), bookId)) {
            throw new BorrowingException(HttpStatus.CONFLICT,
                    "You already have a pending request for this book");
        }
        if (loanDAO.existsActiveForMemberAndBook(member.getId(), bookId)) {
            throw new BorrowingException(HttpStatus.CONFLICT, "You already have this book on loan");
        }
        if (loanDAO.countActiveByMember(member.getId()) >= appConfig.getMaxActiveLoans()) {
            throw new BorrowingException(HttpStatus.CONFLICT,
                    "You have reached the maximum of " + appConfig.getMaxActiveLoans() + " active loans");
        }

        // Physical copies are allocated at approval time only. If none is available
        // then, the request stays PENDING and approval reports a conflict.
        BorrowRequest request = new BorrowRequest(book, member, now());
        try {
            request = borrowRequestDAO.saveAndFlush(request);
        } catch (DataIntegrityViolationException exception) {
            throw new BorrowingException(HttpStatus.CONFLICT,
                    "You already have a pending request for this book");
        }
        return toBorrowRequestResponse(request);
    }

    @Transactional(readOnly = true)
    public List<BorrowRequestResponse> getMyRequests() {
        Users member = requireMember();
        return borrowRequestDAO.findByMember(member.getId()).stream()
                .map(this::toBorrowRequestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BorrowRequestResponse> getPendingRequests() {
        requireStaff();
        return borrowRequestDAO.findPending().stream()
                .map(this::toBorrowRequestResponse)
                .toList();
    }

    @Transactional
    public BorrowRequestResponse approveRequest(int requestId) {
        Users reviewer = requireStaff();

        BorrowRequest request = borrowRequestDAO.lockById(requestId)
                .orElseThrow(() -> new BorrowingException(HttpStatus.NOT_FOUND, "Borrow request not found"));

        if (request.getStatus() != BorrowRequestStatus.PENDING) {
            throw new BorrowingException(HttpStatus.CONFLICT, "Borrow request is not pending");
        }

        Users member = request.getMember();
        if (member == null || member.getRole() != Role.MEMBER) {
            throw new BorrowingException(HttpStatus.CONFLICT, "The requesting user is not a MEMBER");
        }
        if (loanDAO.countActiveByMember(member.getId()) >= appConfig.getMaxActiveLoans()) {
            throw new BorrowingException(HttpStatus.CONFLICT,
                    "The member has reached the maximum of " + appConfig.getMaxActiveLoans()
                            + " active loans");
        }

        BookCopy copy = bookCopyDAO.lockFirstAvailableForBook(request.getBook().getId())
                .orElseThrow(() -> new BorrowingException(HttpStatus.CONFLICT,
                        "No available copy for this book. The request remains pending."));

        Instant issuedAt = now();
        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setMember(member);
        loan.setIssuedBy(reviewer);
        loan.setIssueDate(issuedAt);
        loan.setDueDate(issuedAt.plus(appConfig.getLoanPeriodDays(), ChronoUnit.DAYS));
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setLegacy(false);

        try {
            loan = loanDAO.saveAndFlush(loan);
        } catch (DataIntegrityViolationException exception) {
            throw new BorrowingException(HttpStatus.CONFLICT,
                    "The selected copy was just issued. The request remains pending.");
        }

        copy.setStatus(BookCopyStatus.ON_LOAN);
        bookCopyDAO.save(copy);

        request.setStatus(BorrowRequestStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(issuedAt);
        request.setLoan(loan);
        borrowRequestDAO.save(request);

        return toBorrowRequestResponse(request);
    }

    @Transactional
    public BorrowRequestResponse rejectRequest(int requestId) {
        Users reviewer = requireStaff();

        BorrowRequest request = borrowRequestDAO.lockById(requestId)
                .orElseThrow(() -> new BorrowingException(HttpStatus.NOT_FOUND, "Borrow request not found"));

        if (request.getStatus() != BorrowRequestStatus.PENDING) {
            throw new BorrowingException(HttpStatus.CONFLICT, "Only pending requests can be rejected");
        }

        request.setStatus(BorrowRequestStatus.REJECTED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(now());
        borrowRequestDAO.save(request);

        return toBorrowRequestResponse(request);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getActiveLoans() {
        requireStaff();
        return loanDAO.findActiveLoans().stream()
                .map(this::toLoanResponse)
                .toList();
    }

    @Transactional
    public LoanResponse returnLoan(int loanId) {
        requireStaff();

        Loan loan = loanDAO.lockById(loanId)
                .orElseThrow(() -> new BorrowingException(HttpStatus.NOT_FOUND, "Loan not found"));

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new BorrowingException(HttpStatus.CONFLICT, "Only active loans can be returned");
        }

        loan.setReturnedAt(now());
        loan.setStatus(LoanStatus.RETURNED);
        loanDAO.save(loan);

        BookCopy copy = bookCopyDAO.lockById(loan.getCopy().getId())
                .orElseThrow(() -> new BorrowingException(HttpStatus.NOT_FOUND, "Book copy not found"));
        copy.setStatus(BookCopyStatus.AVAILABLE);
        bookCopyDAO.save(copy);

        return toLoanResponse(loan);
    }

    private Users requireMember() {
        Users user = requireCurrentUser();
        if (user.getRole() != Role.MEMBER) {
            throw new BorrowingException(HttpStatus.FORBIDDEN, "Only members can request books");
        }
        return user;
    }

    private Users requireStaff() {
        Users user = requireCurrentUser();
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.LIBRARIAN) {
            throw new BorrowingException(HttpStatus.FORBIDDEN, "ADMIN or LIBRARIAN access required");
        }
        return user;
    }

    private Users requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BorrowingException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            throw new BorrowingException(HttpStatus.FORBIDDEN, "Authenticated email is required");
        }

        Users user = usersDAO.findByEmail(email);
        if (user == null) {
            throw new BorrowingException(HttpStatus.UNAUTHORIZED, "Authenticated user not found");
        }
        return user;
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private BorrowRequestResponse toBorrowRequestResponse(BorrowRequest request) {
        BorrowRequestResponse response = new BorrowRequestResponse();
        response.setRequestId(request.getId());
        response.setBookId(request.getBook().getId());
        response.setBookTitle(request.getBook().getName());
        response.setStatus(request.getStatus().name());
        response.setRequestedAt(request.getRequestedAt());

        Users member = request.getMember();
        if (member != null) {
            response.setMemberId(member.getId());
            response.setMemberName(member.getName());
            if (member.getProfile() != null) {
                response.setMemberEmail(member.getProfile().getEmail());
            }
        }

        Users reviewer = request.getReviewedBy();
        if (reviewer != null) {
            response.setReviewedById(reviewer.getId());
            response.setReviewedByName(reviewer.getName());
        }
        response.setReviewedAt(request.getReviewedAt());

        Loan loan = request.getLoan();
        if (loan != null) {
            response.setLoanId(loan.getId());
            response.setIssueDate(loan.getIssueDate());
            response.setDueDate(loan.getDueDate());
            response.setReturnDate(loan.getReturnedAt());
        }
        return response;
    }

    private LoanResponse toLoanResponse(Loan loan) {
        LoanResponse response = new LoanResponse();
        response.setLoanId(loan.getId());
        response.setIssueDate(loan.getIssueDate());
        response.setDueDate(loan.getDueDate());
        response.setReturnedAt(loan.getReturnedAt());
        response.setStatus(loan.getStatus().name());
        response.setLegacy(loan.isLegacy());

        BookCopy copy = loan.getCopy();
        if (copy != null) {
            response.setCopyId(copy.getId());
            response.setCopyCode(copy.getCopyCode());
            if (copy.getBook() != null) {
                response.setBookId(copy.getBook().getId());
                response.setBookTitle(copy.getBook().getName());
            }
        }

        Users member = loan.getMember();
        if (member != null) {
            response.setMemberId(member.getId());
            response.setMemberName(member.getName());
            if (member.getProfile() != null) {
                response.setMemberEmail(member.getProfile().getEmail());
            }
        }

        Users issuer = loan.getIssuedBy();
        if (issuer != null) {
            response.setIssuedById(issuer.getId());
            response.setIssuedByName(issuer.getName());
        }
        return response;
    }
}
