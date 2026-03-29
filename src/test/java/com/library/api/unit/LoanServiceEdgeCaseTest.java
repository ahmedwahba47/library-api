package com.library.api.unit;

import com.library.api.dto.LoanCreateRequest;
import com.library.api.dto.LoanDTO;
import com.library.api.entity.Book;
import com.library.api.entity.Loan;
import com.library.api.entity.Loan.LoanStatus;
import com.library.api.exception.BusinessException;
import com.library.api.exception.ResourceNotFoundException;
import com.library.api.mapper.LoanMapper;
import com.library.api.repository.LoanRepository;
import com.library.api.service.BookService;
import com.library.api.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService Edge Case Tests")
class LoanServiceEdgeCaseTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanMapper loanMapper;

    @Mock
    private BookService bookService;

    @InjectMocks
    private LoanService loanService;

    private Book testBook;
    private Loan testLoan;

    @BeforeEach
    void setUp() {
        testBook = Book.builder()
                .id(1L)
                .title("Test Book")
                .author("Test Author")
                .isbn("978-0000000001")
                .totalCopies(5)
                .availableCopies(4)
                .loans(new ArrayList<>())
                .build();

        testLoan = Loan.builder()
                .id(1L)
                .book(testBook)
                .borrowerName("John Doe")
                .borrowerEmail("john@example.com")
                .loanDate(LocalDate.now())
                .dueDate(LocalDate.now().plusWeeks(2))
                .status(LoanStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("createLoan edge cases")
    class CreateLoanEdgeCases {

        @Test
        @DisplayName("should fail when book has exactly 0 available copies")
        void shouldFailWhenZeroCopies() {
            testBook.setAvailableCopies(0);
            when(bookService.findBookById(1L)).thenReturn(testBook);

            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("Jane")
                    .borrowerEmail("jane@example.com")
                    .dueDate(LocalDate.now().plusWeeks(2))
                    .build();

            assertThatThrownBy(() -> loanService.createLoan(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No copies available");
        }

        @Test
        @DisplayName("should succeed when book has exactly 1 available copy")
        void shouldSucceedWithLastCopy() {
            testBook.setAvailableCopies(1);
            when(bookService.findBookById(1L)).thenReturn(testBook);

            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("Jane")
                    .borrowerEmail("jane@example.com")
                    .dueDate(LocalDate.now().plusWeeks(2))
                    .build();

            Loan newLoan = Loan.builder()
                    .id(2L)
                    .book(testBook)
                    .borrowerName("Jane")
                    .status(LoanStatus.ACTIVE)
                    .build();

            LoanDTO expectedDTO = LoanDTO.builder()
                    .id(2L)
                    .borrowerName("Jane")
                    .status(LoanStatus.ACTIVE)
                    .build();

            when(loanMapper.toEntity(request, testBook)).thenReturn(newLoan);
            doNothing().when(bookService).decrementAvailableCopies(testBook);
            when(loanRepository.save(newLoan)).thenReturn(newLoan);
            when(loanMapper.toDTO(newLoan)).thenReturn(expectedDTO);

            LoanDTO result = loanService.createLoan(1L, request);

            assertThat(result.getBorrowerName()).isEqualTo("Jane");
            verify(bookService).decrementAvailableCopies(testBook);
        }
    }

    @Nested
    @DisplayName("returnLoan edge cases")
    class ReturnLoanEdgeCases {

        @Test
        @DisplayName("should reject returning an already returned loan")
        void shouldRejectDoubleReturn() {
            testLoan.setStatus(LoanStatus.RETURNED);
            testLoan.setReturnDate(LocalDate.now().minusDays(1));
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

            assertThatThrownBy(() -> loanService.returnLoan(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already been returned");

            verify(bookService, never()).incrementAvailableCopies(any());
        }

        @Test
        @DisplayName("should allow returning an overdue loan")
        void shouldAllowReturningOverdueLoan() {
            testLoan.setStatus(LoanStatus.OVERDUE);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            doNothing().when(bookService).incrementAvailableCopies(testBook);
            when(loanRepository.save(testLoan)).thenReturn(testLoan);

            LoanDTO returnedDTO = LoanDTO.builder()
                    .id(1L)
                    .status(LoanStatus.RETURNED)
                    .returnDate(LocalDate.now())
                    .build();
            when(loanMapper.toDTO(testLoan)).thenReturn(returnedDTO);

            LoanDTO result = loanService.returnLoan(1L);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.RETURNED);
            verify(bookService).incrementAvailableCopies(testBook);
        }
    }

    @Nested
    @DisplayName("updateLoan edge cases")
    class UpdateLoanEdgeCases {

        @Test
        @DisplayName("should reject updating RETURNED loan")
        void shouldRejectUpdatingReturnedLoan() {
            testLoan.setStatus(LoanStatus.RETURNED);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("Updated Name")
                    .borrowerEmail("updated@example.com")
                    .dueDate(LocalDate.now().plusWeeks(4))
                    .build();

            assertThatThrownBy(() -> loanService.updateLoan(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot update a returned loan");
        }
    }

    @Nested
    @DisplayName("deleteLoan edge cases")
    class DeleteLoanEdgeCases {

        @Test
        @DisplayName("should delete overdue loan and increment available copies")
        void shouldDeleteOverdueLoanAndIncrementCopies() {
            testLoan.setStatus(LoanStatus.OVERDUE);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            doNothing().when(bookService).incrementAvailableCopies(testBook);
            doNothing().when(loanRepository).delete(testLoan);

            loanService.deleteLoan(1L);

            verify(bookService).incrementAvailableCopies(testBook);
            verify(loanRepository).delete(testLoan);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when loan not found")
        void shouldThrowWhenLoanNotFound() {
            when(loanRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.deleteLoan(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Loan not found with id: 999");
        }
    }

    @Nested
    @DisplayName("getLoansForBook edge cases")
    class GetLoansForBookEdgeCases {

        @Test
        @DisplayName("should throw ResourceNotFoundException when book not found")
        void shouldThrowWhenBookNotFound() {
            when(bookService.findBookById(999L))
                    .thenThrow(new ResourceNotFoundException("Book", 999L));

            assertThatThrownBy(() -> loanService.getLoansForBook(999L, PageRequest.of(0, 10)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Book not found with id: 999");
        }
    }

    @Nested
    @DisplayName("returnLoan edge cases - not found")
    class ReturnLoanNotFound {

        @Test
        @DisplayName("should throw ResourceNotFoundException when loan not found")
        void shouldThrowWhenLoanNotFound() {
            when(loanRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.returnLoan(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Loan not found with id: 999");
        }
    }

    @Nested
    @DisplayName("createLoan edge cases - book not found")
    class CreateLoanBookNotFound {

        @Test
        @DisplayName("should throw ResourceNotFoundException when book not found")
        void shouldThrowWhenBookNotFound() {
            when(bookService.findBookById(999L))
                    .thenThrow(new ResourceNotFoundException("Book", 999L));

            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("Jane")
                    .borrowerEmail("jane@example.com")
                    .dueDate(LocalDate.now().plusWeeks(2))
                    .build();

            assertThatThrownBy(() -> loanService.createLoan(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Book not found with id: 999");
        }
    }

    @Nested
    @DisplayName("updateLoan edge cases - overdue")
    class UpdateLoanOverdue {

        @Test
        @DisplayName("should allow updating an overdue loan")
        void shouldAllowUpdatingOverdueLoan() {
            testLoan.setStatus(LoanStatus.OVERDUE);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            when(loanRepository.save(testLoan)).thenReturn(testLoan);

            LoanDTO updatedDTO = LoanDTO.builder()
                    .id(1L)
                    .borrowerName("Updated Name")
                    .status(LoanStatus.OVERDUE)
                    .build();
            when(loanMapper.toDTO(testLoan)).thenReturn(updatedDTO);

            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("Updated Name")
                    .borrowerEmail("updated@example.com")
                    .dueDate(LocalDate.now().plusWeeks(4))
                    .build();

            LoanDTO result = loanService.updateLoan(1L, request);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.OVERDUE);
            verify(loanMapper).updateEntity(testLoan, request);
        }
    }

    @Nested
    @DisplayName("updateOverdueLoans edge cases")
    class UpdateOverdueLoansEdgeCases {

        @Test
        @DisplayName("should do nothing when no overdue loans exist")
        void shouldDoNothingWhenNoOverdueLoans() {
            when(loanRepository.findOverdueLoans(LocalDate.now())).thenReturn(List.of());

            loanService.updateOverdueLoans();

            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("should update multiple overdue loans")
        void shouldUpdateMultipleOverdueLoans() {
            Loan overdue1 = Loan.builder()
                    .id(10L)
                    .status(LoanStatus.ACTIVE)
                    .dueDate(LocalDate.now().minusDays(1))
                    .build();
            Loan overdue2 = Loan.builder()
                    .id(11L)
                    .status(LoanStatus.ACTIVE)
                    .dueDate(LocalDate.now().minusDays(5))
                    .build();

            when(loanRepository.findOverdueLoans(LocalDate.now())).thenReturn(List.of(overdue1, overdue2));
            when(loanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            loanService.updateOverdueLoans();

            assertThat(overdue1.getStatus()).isEqualTo(LoanStatus.OVERDUE);
            assertThat(overdue2.getStatus()).isEqualTo(LoanStatus.OVERDUE);
            verify(loanRepository, times(2)).save(any());
        }
    }
}
