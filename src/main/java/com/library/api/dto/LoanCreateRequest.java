package com.library.api.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class LoanCreateRequest {

    @NotBlank(message = "Borrower name is required")
    @Size(max = 255, message = "Borrower name must not exceed 255 characters")
    private String borrowerName;

    @NotBlank(message = "Borrower email is required")
    @Email(message = "Invalid email format")
    private String borrowerEmail;

    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private LocalDate dueDate;

    public LoanCreateRequest() {
    }

    public LoanCreateRequest(String borrowerName, String borrowerEmail, LocalDate dueDate) {
        this.borrowerName = borrowerName;
        this.borrowerEmail = borrowerEmail;
        this.dueDate = dueDate;
    }

    // Getters and Setters
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

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String borrowerName;
        private String borrowerEmail;
        private LocalDate dueDate;

        public Builder borrowerName(String borrowerName) {
            this.borrowerName = borrowerName;
            return this;
        }

        public Builder borrowerEmail(String borrowerEmail) {
            this.borrowerEmail = borrowerEmail;
            return this;
        }

        public Builder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public LoanCreateRequest build() {
            return new LoanCreateRequest(borrowerName, borrowerEmail, dueDate);
        }
    }
}
