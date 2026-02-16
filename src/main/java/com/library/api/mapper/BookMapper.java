package com.library.api.mapper;

import com.library.api.dto.BookCreateRequest;
import com.library.api.dto.BookDTO;
import com.library.api.entity.Book;
import com.library.api.entity.Loan;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public BookDTO toDTO(Book book) {
        if (book == null) {
            return null;
        }

        long activeLoansCount = book.getLoans().stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.ACTIVE ||
                        loan.getStatus() == Loan.LoanStatus.OVERDUE)
                .count();

        return BookDTO.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .publishedDate(book.getPublishedDate())
                .genre(book.getGenre())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .activeLoansCount((int) activeLoansCount)
                .build();
    }

    public Book toEntity(BookCreateRequest request) {
        if (request == null) {
            return null;
        }

        return Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .publishedDate(request.getPublishedDate())
                .genre(request.getGenre())
                .totalCopies(request.getTotalCopies())
                .availableCopies(request.getTotalCopies())
                .build();
    }

    public void updateEntity(Book book, BookCreateRequest request) {
        if (book == null || request == null) {
            return;
        }

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublishedDate(request.getPublishedDate());
        book.setGenre(request.getGenre());

        int copiesDifference = request.getTotalCopies() - book.getTotalCopies();
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(book.getAvailableCopies() + copiesDifference);
    }
}
