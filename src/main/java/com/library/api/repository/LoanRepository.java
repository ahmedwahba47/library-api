package com.library.api.repository;

import com.library.api.entity.Loan;
import com.library.api.entity.Loan.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Page<Loan> findByBookId(Long bookId, Pageable pageable);

    List<Loan> findByBookId(Long bookId);

    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.loanDate >= :startDate AND l.loanDate <= :endDate")
    Page<Loan> findByLoanDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE " +
            "(:status IS NULL OR l.status = :status) AND " +
            "(:startDate IS NULL OR l.loanDate >= :startDate) AND " +
            "(:endDate IS NULL OR l.loanDate <= :endDate)")
    Page<Loan> searchLoans(
            @Param("status") LoanStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate < :today")
    List<Loan> findOverdueLoans(@Param("today") LocalDate today);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.book.id = :bookId AND l.status IN ('ACTIVE', 'OVERDUE')")
    long countActiveLoansForBook(@Param("bookId") Long bookId);
}
