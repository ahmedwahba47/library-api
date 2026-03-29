package com.library.api.unit;

import com.library.api.dto.LoanCreateRequest;
import com.library.api.dto.LoanDTO;
import com.library.api.entity.Book;
import com.library.api.entity.Loan;
import com.library.api.entity.Loan.LoanStatus;
import com.library.api.mapper.LoanMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoanMapper Unit Tests")
class LoanMapperTest {

    private LoanMapper loanMapper;

    @BeforeEach
    void setUp() {
        loanMapper = new LoanMapper();
    }

    @Nested
    @DisplayName("toDTO")
    class ToDTO {

        @Test
        @DisplayName("should map all loan fields including book info")
        void shouldMapAllFields() {
            Book book = Book.builder()
                    .id(1L).title("Test Book").author("Author")
                    .isbn("978-0000000001").totalCopies(5).availableCopies(4)
                    .loans(new ArrayList<>())
                    .build();

            Loan loan = Loan.builder()
                    .id(10L).book(book)
                    .borrowerName("John Doe").borrowerEmail("john@example.com")
                    .loanDate(LocalDate.of(2024, 1, 1))
                    .dueDate(LocalDate.of(2024, 1, 15))
                    .returnDate(LocalDate.of(2024, 1, 10))
                    .status(LoanStatus.RETURNED)
                    .build();

            LoanDTO dto = loanMapper.toDTO(loan);

            assertThat(dto.getId()).isEqualTo(10L);
            assertThat(dto.getBookId()).isEqualTo(1L);
            assertThat(dto.getBookTitle()).isEqualTo("Test Book");
            assertThat(dto.getBookIsbn()).isEqualTo("978-0000000001");
            assertThat(dto.getBorrowerName()).isEqualTo("John Doe");
            assertThat(dto.getBorrowerEmail()).isEqualTo("john@example.com");
            assertThat(dto.getLoanDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(dto.getDueDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(dto.getReturnDate()).isEqualTo(LocalDate.of(2024, 1, 10));
            assertThat(dto.getStatus()).isEqualTo(LoanStatus.RETURNED);
        }

        @Test
        @DisplayName("should return null when loan is null")
        void shouldReturnNullForNullLoan() {
            assertThat(loanMapper.toDTO(null)).isNull();
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should set loan date to today and status to ACTIVE")
        void shouldSetLoanDateAndActiveStatus() {
            Book book = Book.builder()
                    .id(1L).title("Test Book").author("Author")
                    .isbn("978-0000000001").totalCopies(5).availableCopies(4)
                    .loans(new ArrayList<>())
                    .build();

            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("Jane Doe")
                    .borrowerEmail("jane@example.com")
                    .dueDate(LocalDate.now().plusWeeks(2))
                    .build();

            Loan loan = loanMapper.toEntity(request, book);

            assertThat(loan.getLoanDate()).isEqualTo(LocalDate.now());
            assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
            assertThat(loan.getBook()).isEqualTo(book);
            assertThat(loan.getBorrowerName()).isEqualTo("Jane Doe");
        }

        @Test
        @DisplayName("should return null when request is null")
        void shouldReturnNullForNullRequest() {
            assertThat(loanMapper.toEntity(null, null)).isNull();
        }
    }

    @Nested
    @DisplayName("updateEntity")
    class UpdateEntity {

        @Test
        @DisplayName("should update only borrower fields and due date")
        void shouldUpdateOnlyAllowedFields() {
            Book book = Book.builder()
                    .id(1L).title("Test Book").author("Author")
                    .isbn("978-0000000001").totalCopies(5).availableCopies(4)
                    .loans(new ArrayList<>())
                    .build();

            Loan loan = Loan.builder()
                    .id(1L).book(book)
                    .borrowerName("Old Name").borrowerEmail("old@example.com")
                    .loanDate(LocalDate.of(2024, 1, 1))
                    .dueDate(LocalDate.of(2024, 1, 15))
                    .status(LoanStatus.ACTIVE)
                    .build();

            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("New Name")
                    .borrowerEmail("new@example.com")
                    .dueDate(LocalDate.of(2024, 2, 15))
                    .build();

            loanMapper.updateEntity(loan, request);

            assertThat(loan.getBorrowerName()).isEqualTo("New Name");
            assertThat(loan.getBorrowerEmail()).isEqualTo("new@example.com");
            assertThat(loan.getDueDate()).isEqualTo(LocalDate.of(2024, 2, 15));
            // These should NOT change
            assertThat(loan.getLoanDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        }
    }
}
