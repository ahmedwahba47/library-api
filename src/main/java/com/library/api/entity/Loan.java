package com.library.api.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "borrower_name", nullable = false)
    private String borrowerName;

    @Column(name = "borrower_email", nullable = false)
    private String borrowerEmail;

    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    public enum LoanStatus {
        ACTIVE,
        RETURNED,
        OVERDUE
    }

    public Loan() {
    }

    public Loan(Long id, Book book, String borrowerName, String borrowerEmail,
                LocalDate loanDate, LocalDate dueDate, LocalDate returnDate, LoanStatus status) {
        this.id = id;
        this.book = book;
        this.borrowerName = borrowerName;
        this.borrowerEmail = borrowerEmail;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public void setBorrowerName(String borrowerName) {
        this.borrowerName = borrowerName;
    }

    public String getBorrowerEmail() {
        return borrowerEmail;
    }

    public void setBorrowerEmail(String borrowerEmail) {
        this.borrowerEmail = borrowerEmail;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDate loanDate) {
        this.loanDate = loanDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Book book;
        private String borrowerName;
        private String borrowerEmail;
        private LocalDate loanDate;
        private LocalDate dueDate;
        private LocalDate returnDate;
        private LoanStatus status;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder book(Book book) {
            this.book = book;
            return this;
        }

        public Builder borrowerName(String borrowerName) {
            this.borrowerName = borrowerName;
            return this;
        }

        public Builder borrowerEmail(String borrowerEmail) {
            this.borrowerEmail = borrowerEmail;
            return this;
        }

        public Builder loanDate(LocalDate loanDate) {
            this.loanDate = loanDate;
            return this;
        }

        public Builder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder returnDate(LocalDate returnDate) {
            this.returnDate = returnDate;
            return this;
        }

        public Builder status(LoanStatus status) {
            this.status = status;
            return this;
        }

        public Loan build() {
            return new Loan(id, book, borrowerName, borrowerEmail, loanDate, dueDate, returnDate, status);
        }
    }
}
