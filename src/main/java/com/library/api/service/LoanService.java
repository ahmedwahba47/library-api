package com.library.api.service;

import com.library.api.dto.LoanCreateRequest;
import com.library.api.dto.LoanDTO;
import com.library.api.entity.Book;
import com.library.api.entity.Loan;
import com.library.api.entity.Loan.LoanStatus;
import com.library.api.exception.BusinessException;
import com.library.api.exception.ResourceNotFoundException;
import com.library.api.mapper.LoanMapper;
import com.library.api.repository.LoanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;
    private final BookService bookService;

    public LoanService(LoanRepository loanRepository, LoanMapper loanMapper, BookService bookService) {
        this.loanRepository = loanRepository;
        this.loanMapper = loanMapper;
        this.bookService = bookService;
    }

    @Transactional(readOnly = true)
    public Page<LoanDTO> getAllLoans(Pageable pageable) {
        return loanRepository.findAll(pageable)
                .map(loanMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public LoanDTO getLoanById(Long id) {
        Loan loan = findLoanById(id);
        return loanMapper.toDTO(loan);
    }

    @Transactional(readOnly = true)
    public Page<LoanDTO> getLoansForBook(Long bookId, Pageable pageable) {
        bookService.findBookById(bookId);
        return loanRepository.findByBookId(bookId, pageable)
                .map(loanMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LoanDTO> searchLoans(LoanStatus status, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return loanRepository.searchLoans(status, startDate, endDate, pageable)
                .map(loanMapper::toDTO);
    }

    public LoanDTO createLoan(Long bookId, LoanCreateRequest request) {
        Book book = bookService.findBookById(bookId);

        if (book.getAvailableCopies() <= 0) {
            throw new BusinessException("No copies available for book: " + book.getTitle());
        }

        Loan loan = loanMapper.toEntity(request, book);
        bookService.decrementAvailableCopies(book);

        loan = loanRepository.save(loan);
        return loanMapper.toDTO(loan);
    }

    public LoanDTO updateLoan(Long id, LoanCreateRequest request) {
        Loan loan = findLoanById(id);

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException("Cannot update a returned loan");
        }

        loanMapper.updateEntity(loan, request);
        loan = loanRepository.save(loan);
        return loanMapper.toDTO(loan);
    }

    public LoanDTO returnLoan(Long id) {
        Loan loan = findLoanById(id);

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException("Loan has already been returned");
        }

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDate.now());
        bookService.incrementAvailableCopies(loan.getBook());

        loan = loanRepository.save(loan);
        return loanMapper.toDTO(loan);
    }

    public void deleteLoan(Long id) {
        Loan loan = findLoanById(id);

        if (loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.OVERDUE) {
            bookService.incrementAvailableCopies(loan.getBook());
        }

        loanRepository.delete(loan);
    }

    public void updateOverdueLoans() {
        List<Loan> overdueLoans = loanRepository.findOverdueLoans(LocalDate.now());
        for (Loan loan : overdueLoans) {
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);
        }
    }

    private Loan findLoanById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
    }
}
