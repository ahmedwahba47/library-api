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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService Unit Tests")
class LoanServiceTest {

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
    private LoanDTO testLoanDTO;
    private LoanCreateRequest testLoanRequest;

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

        testLoanDTO = LoanDTO.builder()
                .id(1L)
                .bookId(1L)
                .bookTitle("Test Book")
                .bookIsbn("978-0000000001")
                .borrowerName("John Doe")
                .borrowerEmail("john@example.com")
                .loanDate(LocalDate.now())
                .dueDate(LocalDate.now().plusWeeks(2))
                .status(LoanStatus.ACTIVE)
                .build();

        testLoanRequest = LoanCreateRequest.builder()
                .borrowerName("John Doe")
                .borrowerEmail("john@example.com")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build();
    }

    @Nested
    @DisplayName("getAllLoans")
    class GetAllLoans {

        @Test
        @DisplayName("should return paginated loans")
        void shouldReturnPaginatedLoans() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Loan> loanPage = new PageImpl<>(List.of(testLoan));

            when(loanRepository.findAll(pageable)).thenReturn(loanPage);
            when(loanMapper.toDTO(testLoan)).thenReturn(testLoanDTO);

            Page<LoanDTO> result = loanService.getAllLoans(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getBorrowerName()).isEqualTo("John Doe");
        }
    }

    @Nested
    @DisplayName("getLoanById")
    class GetLoanById {

        @Test
        @DisplayName("should return loan when found")
        void shouldReturnLoanWhenFound() {
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            when(loanMapper.toDTO(testLoan)).thenReturn(testLoanDTO);

            LoanDTO result = loanService.getLoanById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getBorrowerName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when loan not found")
        void shouldThrowExceptionWhenLoanNotFound() {
            when(loanRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.getLoanById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Loan not found with id: 999");
        }
    }

    @Nested
    @DisplayName("createLoan")
    class CreateLoan {

        @Test
        @DisplayName("should create loan successfully")
        void shouldCreateLoanSuccessfully() {
            when(bookService.findBookById(1L)).thenReturn(testBook);
            when(loanMapper.toEntity(testLoanRequest, testBook)).thenReturn(testLoan);
            when(loanRepository.save(testLoan)).thenReturn(testLoan);
            when(loanMapper.toDTO(testLoan)).thenReturn(testLoanDTO);
            doNothing().when(bookService).decrementAvailableCopies(testBook);

            LoanDTO result = loanService.createLoan(1L, testLoanRequest);

            assertThat(result.getBorrowerName()).isEqualTo("John Doe");
            verify(bookService).decrementAvailableCopies(testBook);
        }

        @Test
        @DisplayName("should throw BusinessException when no copies available")
        void shouldThrowExceptionWhenNoCopiesAvailable() {
            testBook.setAvailableCopies(0);
            when(bookService.findBookById(1L)).thenReturn(testBook);

            assertThatThrownBy(() -> loanService.createLoan(1L, testLoanRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No copies available");
        }
    }

    @Nested
    @DisplayName("updateLoan")
    class UpdateLoan {

        @Test
        @DisplayName("should update loan successfully")
        void shouldUpdateLoanSuccessfully() {
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            when(loanRepository.save(testLoan)).thenReturn(testLoan);
            when(loanMapper.toDTO(testLoan)).thenReturn(testLoanDTO);

            LoanDTO result = loanService.updateLoan(1L, testLoanRequest);

            assertThat(result).isNotNull();
            verify(loanMapper).updateEntity(testLoan, testLoanRequest);
        }

        @Test
        @DisplayName("should throw BusinessException when loan already returned")
        void shouldThrowExceptionWhenLoanReturned() {
            testLoan.setStatus(LoanStatus.RETURNED);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

            assertThatThrownBy(() -> loanService.updateLoan(1L, testLoanRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot update a returned loan");
        }
    }

    @Nested
    @DisplayName("returnLoan")
    class ReturnLoan {

        @Test
        @DisplayName("should return loan successfully")
        void shouldReturnLoanSuccessfully() {
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            doNothing().when(bookService).incrementAvailableCopies(testBook);
            when(loanRepository.save(testLoan)).thenReturn(testLoan);

            LoanDTO returnedLoanDTO = LoanDTO.builder()
                    .id(1L)
                    .status(LoanStatus.RETURNED)
                    .returnDate(LocalDate.now())
                    .build();
            when(loanMapper.toDTO(testLoan)).thenReturn(returnedLoanDTO);

            LoanDTO result = loanService.returnLoan(1L);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.RETURNED);
            verify(bookService).incrementAvailableCopies(testBook);
        }

        @Test
        @DisplayName("should throw BusinessException when loan already returned")
        void shouldThrowExceptionWhenLoanAlreadyReturned() {
            testLoan.setStatus(LoanStatus.RETURNED);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

            assertThatThrownBy(() -> loanService.returnLoan(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already been returned");
        }
    }

    @Nested
    @DisplayName("deleteLoan")
    class DeleteLoan {

        @Test
        @DisplayName("should delete active loan and increment available copies")
        void shouldDeleteActiveLoanAndIncrementCopies() {
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            doNothing().when(bookService).incrementAvailableCopies(testBook);
            doNothing().when(loanRepository).delete(testLoan);

            loanService.deleteLoan(1L);

            verify(bookService).incrementAvailableCopies(testBook);
            verify(loanRepository).delete(testLoan);
        }

        @Test
        @DisplayName("should delete returned loan without incrementing copies")
        void shouldDeleteReturnedLoanWithoutIncrementingCopies() {
            testLoan.setStatus(LoanStatus.RETURNED);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            doNothing().when(loanRepository).delete(testLoan);

            loanService.deleteLoan(1L);

            verify(bookService, never()).incrementAvailableCopies(any());
            verify(loanRepository).delete(testLoan);
        }
    }

    @Nested
    @DisplayName("searchLoans")
    class SearchLoans {

        @Test
        @DisplayName("should search loans by status and date range")
        void shouldSearchLoansByStatusAndDateRange() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Loan> loanPage = new PageImpl<>(List.of(testLoan));
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();

            when(loanRepository.searchLoans(LoanStatus.ACTIVE, startDate, endDate, pageable))
                    .thenReturn(loanPage);
            when(loanMapper.toDTO(testLoan)).thenReturn(testLoanDTO);

            Page<LoanDTO> result = loanService.searchLoans(LoanStatus.ACTIVE, startDate, endDate, pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getLoansForBook")
    class GetLoansForBook {

        @Test
        @DisplayName("should return loans for specific book")
        void shouldReturnLoansForBook() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Loan> loanPage = new PageImpl<>(List.of(testLoan));

            when(bookService.findBookById(1L)).thenReturn(testBook);
            when(loanRepository.findByBookId(1L, pageable)).thenReturn(loanPage);
            when(loanMapper.toDTO(testLoan)).thenReturn(testLoanDTO);

            Page<LoanDTO> result = loanService.getLoansForBook(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getBookId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("updateOverdueLoans")
    class UpdateOverdueLoans {

        @Test
        @DisplayName("should update overdue loans status")
        void shouldUpdateOverdueLoansStatus() {
            Loan overdueLoan = Loan.builder()
                    .id(2L)
                    .status(LoanStatus.ACTIVE)
                    .dueDate(LocalDate.now().minusDays(1))
                    .build();

            when(loanRepository.findOverdueLoans(LocalDate.now())).thenReturn(List.of(overdueLoan));
            when(loanRepository.save(overdueLoan)).thenReturn(overdueLoan);

            loanService.updateOverdueLoans();

            assertThat(overdueLoan.getStatus()).isEqualTo(LoanStatus.OVERDUE);
            verify(loanRepository).save(overdueLoan);
        }
    }
}
