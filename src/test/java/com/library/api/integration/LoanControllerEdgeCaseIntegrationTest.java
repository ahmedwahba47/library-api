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
@DisplayName("LoanController Edge Case Integration Tests")
class LoanControllerEdgeCaseIntegrationTest {

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
                .title("Edge Case Book")
                .author("Test Author")
                .isbn("9781111111111")
                .publishedDate(LocalDate.of(2023, 1, 1))
                .genre("Fiction")
                .totalCopies(2)
                .availableCopies(1)
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
    @DisplayName("Loan Validation Edge Cases")
    class LoanValidationEdgeCases {

        @Test
        @DisplayName("should return 400 when borrowerName is empty")
        void shouldReturn400WhenBorrowerNameEmpty() throws Exception {
            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("")
                    .borrowerEmail("valid@example.com")
                    .dueDate(LocalDate.now().plusWeeks(2))
                    .build();

            mockMvc.perform(post("/api/books/{bookId}/loans", testBook.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors").exists());
        }

        @Test
        @DisplayName("should return 400 when dueDate is in the past")
        void shouldReturn400WhenDueDatePast() throws Exception {
            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("Valid Name")
                    .borrowerEmail("valid@example.com")
                    .dueDate(LocalDate.now().minusDays(1))
                    .build();

            mockMvc.perform(post("/api/books/{bookId}/loans", testBook.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Available Copies Business Logic")
    class AvailableCopiesLogic {

        @Test
        @DisplayName("should decrement available copies after creating loan")
        void shouldDecrementAfterLoanCreation() throws Exception {
            LoanCreateRequest request = LoanCreateRequest.builder()
                    .borrowerName("Jane Smith")
                    .borrowerEmail("jane@example.com")
                    .dueDate(LocalDate.now().plusWeeks(3))
                    .build();

            mockMvc.perform(post("/api/books/{bookId}/loans", testBook.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/books/{id}", testBook.getId()))
                    .andExpect(jsonPath("$.availableCopies").value(0));
        }

        @Test
        @DisplayName("should return 400 when all copies are loaned out")
        void shouldReturn400WhenAllCopiesLoaned() throws Exception {
            LoanCreateRequest request1 = LoanCreateRequest.builder()
                    .borrowerName("Jane Smith")
                    .borrowerEmail("jane@example.com")
                    .dueDate(LocalDate.now().plusWeeks(3))
                    .build();

            mockMvc.perform(post("/api/books/{bookId}/loans", testBook.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            LoanCreateRequest request2 = LoanCreateRequest.builder()
                    .borrowerName("Bob Jones")
                    .borrowerEmail("bob@example.com")
                    .dueDate(LocalDate.now().plusWeeks(2))
                    .build();

            mockMvc.perform(post("/api/books/{bookId}/loans", testBook.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("No copies available")));
        }

        @Test
        @DisplayName("should increment available copies after returning loan")
        void shouldIncrementAfterReturn() throws Exception {
            mockMvc.perform(put("/api/loans/{id}/return", testLoan.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RETURNED"));

            mockMvc.perform(get("/api/books/{id}", testBook.getId()))
                    .andExpect(jsonPath("$.availableCopies").value(2));
        }
    }

    @Nested
    @DisplayName("Return Loan Edge Cases")
    class ReturnLoanEdgeCases {

        @Test
        @DisplayName("should return 400 when returning already returned loan")
        void shouldReturn400ForDoubleReturn() throws Exception {
            mockMvc.perform(put("/api/loans/{id}/return", testLoan.getId()))
                    .andExpect(status().isOk());

            mockMvc.perform(put("/api/loans/{id}/return", testLoan.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("already been returned")));
        }
    }
}
