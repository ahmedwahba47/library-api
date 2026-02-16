package com.library.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.api.dto.LoanCreateRequest;
import com.library.api.entity.Book;
import com.library.api.entity.Loan;
import com.library.api.entity.Loan.LoanStatus;
import com.library.api.repository.BookRepository;
import com.library.api.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("LoanController Integration Tests")
class LoanControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    private Book testBook;
    private Loan testLoan;

    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();

        testBook = Book.builder()
                .title("Test Book")
                .author("Test Author")
                .isbn("9781234567890")
                .publishedDate(LocalDate.of(2023, 1, 1))
                .genre("Fiction")
                .totalCopies(5)
                .availableCopies(4)
                .loans(new ArrayList<>())
                .build();
        testBook = bookRepository.save(testBook);

        testLoan = Loan.builder()
                .book(testBook)
                .borrowerName("John Doe")
                .borrowerEmail("john@example.com")
                .loanDate(LocalDate.now())
                .dueDate(LocalDate.now().plusWeeks(2))
                .status(LoanStatus.ACTIVE)
                .build();
        testLoan = loanRepository.save(testLoan);
    }

    @Nested
    @DisplayName("GET /api/loans")
    class GetAllLoans {

        @Test
        @DisplayName("should return paginated list of loans")
        void shouldReturnPaginatedLoans() throws Exception {
            mockMvc.perform(get("/api/loans")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].borrowerName").value("John Doe"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should filter loans by status")
        void shouldFilterByStatus() throws Exception {
            mockMvc.perform(get("/api/loans")
                            .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
        }

        @Test
        @DisplayName("should filter loans by date range")
        void shouldFilterByDateRange() throws Exception {
            mockMvc.perform(get("/api/loans")
                            .param("startDate", LocalDate.now().minusDays(1).toString())
                            .param("endDate", LocalDate.now().plusDays(1).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/loans/{id}")
    class GetLoanById {

        @Test
        @DisplayName("should return loan when found")
        void shouldReturnLoanWhenFound() throws Exception {
            mockMvc.perform(get("/api/loans/{id}", testLoan.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testLoan.getId()))
                    .andExpect(jsonPath("$.borrowerName").value("John Doe"))
                    .andExpect(jsonPath("$.borrowerEmail").value("john@example.com"))
                    .andExpect(jsonPath("$.bookTitle").value("Test Book"));
        }

        @Test
        @DisplayName("should return 404 when loan not found")
        void shouldReturn404WhenNotFound() throws Exception {
            mockMvc.perform(get("/api/loans/{id}", 99999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value(containsString("Loan not found")));
        }
    }

    @Nested
    @DisplayName("GET /api/books/{id}/loans")
    class GetLoansForBook {

        @Test
        @DisplayName("should return loans for specific book")
        void shouldReturnLoansForBook() throws Exception {
            mockMvc.perform(get("/api/books/{id}/loans", testBook.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].bookId").value(testBook.getId()));
        }

        @Test
        @DisplayName("should return 404 when book not found")
        void shouldReturn404WhenBookNotFound() throws Exception {
            mockMvc.perform(get("/api/books/{id}/loans", 99999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/books/{bookId}/loans")
    class CreateLoan {

        @Test
        @DisplayName("should create loan with valid request")
        void shouldCreateLoanWithValidRequest() throws Exception {
            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("Jane Smith")
                    .borrowerEmail("jane@example.com")
                    .dueDate(LocalDate.now().plusWeeks(3))
                    .build();

            mockMvc.perform(post("/api/books/{bookId}/loans", testBook.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.borrowerName").value("Jane Smith"))
                    .andExpect(jsonPath("$.borrowerEmail").value("jane@example.com"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.bookId").value(testBook.getId()));
        }

        @Test
        @DisplayName("should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("")
                    .borrowerEmail("invalid-email")
                    .dueDate(LocalDate.now().minusDays(1))
                    .build();

            mockMvc.perform(post("/api/books/{bookId}/loans", testBook.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors").exists());
        }

        @Test
        @DisplayName("should return 400 when no copies available")
        void shouldReturn400WhenNoCopiesAvailable() throws Exception {
            testBook.setAvailableCopies(0);
            bookRepository.save(testBook);

            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("Jane Smith")
                    .borrowerEmail("jane@example.com")
                    .dueDate(LocalDate.now().plusWeeks(2))
                    .build();

            mockMvc.perform(post("/api/books/{bookId}/loans", testBook.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("No copies available")));
        }

        @Test
        @DisplayName("should return 404 when book not found")
        void shouldReturn404WhenBookNotFound() throws Exception {
            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("Jane Smith")
                    .borrowerEmail("jane@example.com")
                    .dueDate(LocalDate.now().plusWeeks(2))
                    .build();

            mockMvc.perform(post("/api/books/{bookId}/loans", 99999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/loans/{id}")
    class UpdateLoan {

        @Test
        @DisplayName("should update loan with valid request")
        void shouldUpdateLoanWithValidRequest() throws Exception {
            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("Updated Name")
                    .borrowerEmail("updated@example.com")
                    .dueDate(LocalDate.now().plusWeeks(4))
                    .build();

            mockMvc.perform(put("/api/loans/{id}", testLoan.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.borrowerName").value("Updated Name"))
                    .andExpect(jsonPath("$.borrowerEmail").value("updated@example.com"));
        }

        @Test
        @DisplayName("should return 400 when updating returned loan")
        void shouldReturn400WhenUpdatingReturnedLoan() throws Exception {
            testLoan.setStatus(LoanStatus.RETURNED);
            testLoan.setReturnDate(LocalDate.now());
            loanRepository.save(testLoan);

            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("Updated Name")
                    .borrowerEmail("updated@example.com")
                    .dueDate(LocalDate.now().plusWeeks(4))
                    .build();

            mockMvc.perform(put("/api/loans/{id}", testLoan.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Cannot update a returned loan")));
        }
    }

    @Nested
    @DisplayName("PUT /api/loans/{id}/return")
    class ReturnLoan {

        @Test
        @DisplayName("should mark loan as returned")
        void shouldMarkLoanAsReturned() throws Exception {
            mockMvc.perform(put("/api/loans/{id}/return", testLoan.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RETURNED"))
                    .andExpect(jsonPath("$.returnDate").value(LocalDate.now().toString()));
        }

        @Test
        @DisplayName("should return 400 when loan already returned")
        void shouldReturn400WhenAlreadyReturned() throws Exception {
            testLoan.setStatus(LoanStatus.RETURNED);
            testLoan.setReturnDate(LocalDate.now());
            loanRepository.save(testLoan);

            mockMvc.perform(put("/api/loans/{id}/return", testLoan.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("already been returned")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/loans/{id}")
    class DeleteLoan {

        @Test
        @DisplayName("should delete loan successfully")
        void shouldDeleteLoanSuccessfully() throws Exception {
            mockMvc.perform(delete("/api/loans/{id}", testLoan.getId()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/loans/{id}", testLoan.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 when loan not found")
        void shouldReturn404WhenNotFound() throws Exception {
            mockMvc.perform(delete("/api/loans/{id}", 99999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Date Filtering")
    class DateFilteringTests {

        @BeforeEach
        void setUpMultipleLoans() {
            Loan pastLoan = Loan.builder()
                    .book(testBook)
                    .borrowerName("Past Borrower")
                    .borrowerEmail("past@example.com")
                    .loanDate(LocalDate.now().minusDays(30))
                    .dueDate(LocalDate.now().minusDays(16))
                    .status(LoanStatus.RETURNED)
                    .returnDate(LocalDate.now().minusDays(20))
                    .build();
            loanRepository.save(pastLoan);
        }

        @Test
        @DisplayName("should filter loans within date range")
        void shouldFilterLoansWithinDateRange() throws Exception {
            mockMvc.perform(get("/api/loans")
                            .param("startDate", LocalDate.now().minusDays(7).toString())
                            .param("endDate", LocalDate.now().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].borrowerName").value("John Doe"));
        }

        @Test
        @DisplayName("should filter loans by status and date range")
        void shouldFilterByStatusAndDateRange() throws Exception {
            mockMvc.perform(get("/api/loans")
                            .param("status", "RETURNED")
                            .param("startDate", LocalDate.now().minusDays(35).toString())
                            .param("endDate", LocalDate.now().minusDays(25).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].status").value("RETURNED"));
        }
    }

    @Nested
    @DisplayName("Sorting")
    class SortingTests {

        @BeforeEach
        void setUpMultipleLoans() {
            Loan earlierLoan = Loan.builder()
                    .book(testBook)
                    .borrowerName("Earlier Borrower")
                    .borrowerEmail("earlier@example.com")
                    .loanDate(LocalDate.now().minusDays(5))
                    .dueDate(LocalDate.now().plusDays(9))
                    .status(LoanStatus.ACTIVE)
                    .build();
            loanRepository.save(earlierLoan);
        }

        @Test
        @DisplayName("should sort loans by due date ascending")
        void shouldSortByDueDateAscending() throws Exception {
            mockMvc.perform(get("/api/loans")
                            .param("sort", "dueDate,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].borrowerName").value("Earlier Borrower"));
        }

        @Test
        @DisplayName("should sort loans by loan date descending")
        void shouldSortByLoanDateDescending() throws Exception {
            mockMvc.perform(get("/api/loans")
                            .param("sort", "loanDate,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].borrowerName").value("John Doe"));
        }
    }
}
