package com.library.api.mapper;

import com.library.api.dto.LoanCreateRequest;
import com.library.api.dto.LoanDTO;
import com.library.api.entity.Book;
import com.library.api.entity.Loan;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class LoanMapper {

    public LoanDTO toDTO(Loan loan) {
        if (loan == null) {
            return null;
        }

        return LoanDTO.builder()
                .id(loan.getId())
                .bookId(loan.getBook().getId())
                .bookTitle(loan.getBook().getTitle())
                .bookIsbn(loan.getBook().getIsbn())
                .borrowerName(loan.getBorrowerName())
                .borrowerEmail(loan.getBorrowerEmail())
                .loanDate(loan.getLoanDate())
                .dueDate(loan.getDueDate())
                .returnDate(loan.getReturnDate())
                .status(loan.getStatus())
                .build();
    }

    public Loan toEntity(LoanCreateRequest request, Book book) {
        if (request == null || book == null) {
            return null;
        }

        return Loan.builder()
                .book(book)
                .borrowerName(request.getBorrowerName())
                .borrowerEmail(request.getBorrowerEmail())
                .loanDate(LocalDate.now())
                .dueDate(request.getDueDate())
                .status(Loan.LoanStatus.ACTIVE)
                .build();
    }

    public void updateEntity(Loan loan, LoanCreateRequest request) {
        if (loan == null || request == null) {
            return;
        }

        loan.setBorrowerName(request.getBorrowerName());
        loan.setBorrowerEmail(request.getBorrowerEmail());
        loan.setDueDate(request.getDueDate());
    }
}
