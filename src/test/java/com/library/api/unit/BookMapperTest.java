package com.library.api.unit;

import com.library.api.dto.BookCreateRequest;
import com.library.api.dto.BookDTO;
import com.library.api.entity.Book;
import com.library.api.entity.Loan;
import com.library.api.entity.Loan.LoanStatus;
import com.library.api.mapper.BookMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BookMapper Unit Tests")
class BookMapperTest {

    private BookMapper bookMapper;

    @BeforeEach
    void setUp() {
        bookMapper = new BookMapper();
    }

    @Nested
    @DisplayName("toDTO")
    class ToDTO {

        @Test
        @DisplayName("should map all book fields to DTO")
        void shouldMapAllFields() {
            Book book = Book.builder()
                    .id(1L)
                    .title("Clean Code")
                    .author("Robert Martin")
                    .isbn("978-0132350884")
                    .publishedDate(LocalDate.of(2008, 8, 1))
                    .genre("Software")
                    .totalCopies(5)
                    .availableCopies(3)
                    .loans(new ArrayList<>())
                    .build();

            BookDTO dto = bookMapper.toDTO(book);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getTitle()).isEqualTo("Clean Code");
            assertThat(dto.getAuthor()).isEqualTo("Robert Martin");
            assertThat(dto.getIsbn()).isEqualTo("978-0132350884");
            assertThat(dto.getPublishedDate()).isEqualTo(LocalDate.of(2008, 8, 1));
            assertThat(dto.getGenre()).isEqualTo("Software");
            assertThat(dto.getTotalCopies()).isEqualTo(5);
            assertThat(dto.getAvailableCopies()).isEqualTo(3);
        }

        @Test
        @DisplayName("should count only ACTIVE and OVERDUE loans as active")
        void shouldCountOnlyActiveAndOverdueLoans() {
            Book book = Book.builder()
                    .id(1L).title("Test").author("Author").isbn("978-0000000001")
                    .totalCopies(5).availableCopies(2)
                    .loans(new ArrayList<>())
                    .build();

            Loan activeLoan = Loan.builder().id(1L).book(book).status(LoanStatus.ACTIVE).build();
            Loan overdueLoan = Loan.builder().id(2L).book(book).status(LoanStatus.OVERDUE).build();
            Loan returnedLoan = Loan.builder().id(3L).book(book).status(LoanStatus.RETURNED).build();
            book.setLoans(List.of(activeLoan, overdueLoan, returnedLoan));

            BookDTO dto = bookMapper.toDTO(book);

            assertThat(dto.getActiveLoansCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return zero active loans when all returned")
        void shouldReturnZeroActiveLoansWhenAllReturned() {
            Book book = Book.builder()
                    .id(1L).title("Test").author("Author").isbn("978-0000000001")
                    .totalCopies(5).availableCopies(5)
                    .loans(new ArrayList<>())
                    .build();

            Loan returned1 = Loan.builder().id(1L).book(book).status(LoanStatus.RETURNED).build();
            Loan returned2 = Loan.builder().id(2L).book(book).status(LoanStatus.RETURNED).build();
            book.setLoans(List.of(returned1, returned2));

            BookDTO dto = bookMapper.toDTO(book);

            assertThat(dto.getActiveLoansCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return null when book is null")
        void shouldReturnNullForNullBook() {
            assertThat(bookMapper.toDTO(null)).isNull();
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should set availableCopies equal to totalCopies on creation")
        void shouldSetAvailableCopiesEqualToTotal() {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("New Book").author("Author").isbn("978-0000000099")
                    .totalCopies(10).genre("Fiction")
                    .publishedDate(LocalDate.of(2023, 6, 1))
                    .build();

            Book book = bookMapper.toEntity(request);

            assertThat(book.getAvailableCopies()).isEqualTo(10);
            assertThat(book.getTotalCopies()).isEqualTo(10);
        }

        @Test
        @DisplayName("should return null when request is null")
        void shouldReturnNullForNullRequest() {
            assertThat(bookMapper.toEntity(null)).isNull();
        }
    }

    @Nested
    @DisplayName("updateEntity")
    class UpdateEntity {

        @Test
        @DisplayName("should adjust available copies when total copies increases")
        void shouldAdjustAvailableCopiesOnIncrease() {
            Book book = Book.builder()
                    .id(1L).title("Old Title").author("Old Author").isbn("978-0000000001")
                    .totalCopies(5).availableCopies(3)
                    .loans(new ArrayList<>())
                    .build();

            BookCreateRequest request = BookCreateRequest.builder()
                    .title("New Title").author("New Author").isbn("978-0000000001")
                    .totalCopies(8).genre("Fiction")
                    .build();

            bookMapper.updateEntity(book, request);

            assertThat(book.getTotalCopies()).isEqualTo(8);
            assertThat(book.getAvailableCopies()).isEqualTo(6); // 3 + (8-5)
            assertThat(book.getTitle()).isEqualTo("New Title");
        }

        @Test
        @DisplayName("should adjust available copies when total copies decreases")
        void shouldAdjustAvailableCopiesOnDecrease() {
            Book book = Book.builder()
                    .id(1L).title("Old Title").author("Old Author").isbn("978-0000000001")
                    .totalCopies(10).availableCopies(8)
                    .loans(new ArrayList<>())
                    .build();

            BookCreateRequest request = BookCreateRequest.builder()
                    .title("Old Title").author("Old Author").isbn("978-0000000001")
                    .totalCopies(7).genre("Fiction")
                    .build();

            bookMapper.updateEntity(book, request);

            assertThat(book.getTotalCopies()).isEqualTo(7);
            assertThat(book.getAvailableCopies()).isEqualTo(5); // 8 + (7-10)
        }
    }
}
