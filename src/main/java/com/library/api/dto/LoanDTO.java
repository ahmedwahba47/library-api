package com.library.api.dto;

import com.library.api.entity.Loan.LoanStatus;

import java.time.LocalDate;

public class LoanDTO {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookIsbn;
    private String borrowerName;
    private String borrowerEmail;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private LoanStatus status;

    public LoanDTO() {
    }

    public LoanDTO(Long id, Long bookId, String bookTitle, String bookIsbn, String borrowerName,
                   String borrowerEmail, LocalDate loanDate, LocalDate dueDate, LocalDate returnDate,
                   LoanStatus status) {
        this.id = id;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookIsbn = bookIsbn;
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

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookIsbn() {
        return bookIsbn;
    }

    public void setBookIsbn(String bookIsbn) {
        this.bookIsbn = bookIsbn;
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
        private Long bookId;
        private String bookTitle;
        private String bookIsbn;
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

        public Builder bookId(Long bookId) {
            this.bookId = bookId;
            return this;
        }

        public Builder bookTitle(String bookTitle) {
            this.bookTitle = bookTitle;
            return this;
        }

        public Builder bookIsbn(String bookIsbn) {
            this.bookIsbn = bookIsbn;
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

        public LoanDTO build() {
            return new LoanDTO(id, bookId, bookTitle, bookIsbn, borrowerName, borrowerEmail,
                    loanDate, dueDate, returnDate, status);
        }
    }
}
