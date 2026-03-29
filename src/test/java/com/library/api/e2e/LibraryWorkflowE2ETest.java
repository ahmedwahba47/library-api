package com.library.api.e2e;

import com.library.api.dto.BookCreateRequest;
import com.library.api.dto.BookDTO;
import com.library.api.dto.LoanCreateRequest;
import com.library.api.dto.LoanDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End tests using TestRestTemplate with a real embedded server.
 * Unlike MockMvc-based integration tests, these hit an actual HTTP port,
 * exercising the full network stack including serialisation, HTTP headers,
 * and servlet container behaviour.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Library Workflow E2E Tests")
class LibraryWorkflowE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        // Clean state - delete all loans then books via API
        // Get all loans and delete them
        ResponseEntity<Map> loansResponse = restTemplate.getForEntity("/api/loans", Map.class);
        if (loansResponse.getStatusCode() == HttpStatus.OK && loansResponse.getBody() != null) {
            var content = (java.util.List<Map<String, Object>>) loansResponse.getBody().get("content");
            if (content != null) {
                for (Map<String, Object> loan : content) {
                    Number id = (Number) loan.get("id");
                    restTemplate.delete("/api/loans/{id}", id.longValue());
                }
            }
        }
        // Get all books and delete them
        ResponseEntity<Map> booksResponse = restTemplate.getForEntity("/api/books", Map.class);
        if (booksResponse.getStatusCode() == HttpStatus.OK && booksResponse.getBody() != null) {
            var content = (java.util.List<Map<String, Object>>) booksResponse.getBody().get("content");
            if (content != null) {
                for (Map<String, Object> book : content) {
                    Number id = (Number) book.get("id");
                    restTemplate.delete("/api/books/{id}", id.longValue());
                }
            }
        }
    }

    @Test
    @DisplayName("Full lifecycle: create book, loan it, return it, verify copies restored")
    void fullBookLoanReturnLifecycle() {
        // 1. Create a book via POST
        BookCreateRequest bookRequest = BookCreateRequest.builder()
                .title("E2E Test Book")
                .author("E2E Author")
                .isbn("9781234567890")
                .publishedDate(LocalDate.of(2024, 1, 1))
                .genre("Science")
                .totalCopies(3)
                .build();

        ResponseEntity<BookDTO> createBookResponse = restTemplate.postForEntity(
                "/api/books", bookRequest, BookDTO.class);

        assertThat(createBookResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BookDTO createdBook = createBookResponse.getBody();
        assertThat(createdBook).isNotNull();
        assertThat(createdBook.getId()).isNotNull();
        assertThat(createdBook.getAvailableCopies()).isEqualTo(3);

        Long bookId = createdBook.getId();

        // 2. Create a loan for that book via POST
        LoanCreateRequest loanRequest = LoanCreateRequest.builder()
                .borrowerName("Alice")
                .borrowerEmail("alice@example.com")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build();

        ResponseEntity<LoanDTO> createLoanResponse = restTemplate.postForEntity(
                "/api/books/{bookId}/loans", loanRequest, LoanDTO.class, bookId);

        assertThat(createLoanResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LoanDTO createdLoan = createLoanResponse.getBody();
        assertThat(createdLoan).isNotNull();
        assertThat(createdLoan.getBorrowerName()).isEqualTo("Alice");

        Long loanId = createdLoan.getId();

        // 3. Verify available copies decremented
        ResponseEntity<BookDTO> getBookResponse = restTemplate.getForEntity(
                "/api/books/{id}", BookDTO.class, bookId);
        assertThat(getBookResponse.getBody().getAvailableCopies()).isEqualTo(2);

        // 4. Return the loan via PUT
        restTemplate.put("/api/loans/{id}/return", null, loanId);

        // 5. Verify the loan is now RETURNED
        ResponseEntity<LoanDTO> getLoanResponse = restTemplate.getForEntity(
                "/api/loans/{id}", LoanDTO.class, loanId);
        assertThat(getLoanResponse.getBody().getStatus().toString()).isEqualTo("RETURNED");

        // 6. Verify available copies restored
        ResponseEntity<BookDTO> finalBookResponse = restTemplate.getForEntity(
                "/api/books/{id}", BookDTO.class, bookId);
        assertThat(finalBookResponse.getBody().getAvailableCopies()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should prevent loaning when no copies available and verify error response")
    void shouldPreventLoanWhenNoCopiesAvailable() {
        // 1. Create a book with only 1 copy
        BookCreateRequest bookRequest = BookCreateRequest.builder()
                .title("Scarce Book")
                .author("Rare Author")
                .isbn("9780987654321")
                .totalCopies(1)
                .build();

        ResponseEntity<BookDTO> bookResponse = restTemplate.postForEntity(
                "/api/books", bookRequest, BookDTO.class);
        Long bookId = bookResponse.getBody().getId();

        // 2. Loan the only copy
        LoanCreateRequest loanRequest = LoanCreateRequest.builder()
                .borrowerName("Bob")
                .borrowerEmail("bob@example.com")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build();

        ResponseEntity<LoanDTO> loanResponse = restTemplate.postForEntity(
                "/api/books/{bookId}/loans", loanRequest, LoanDTO.class, bookId);
        assertThat(loanResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 3. Try to loan again - should fail with 400
        LoanCreateRequest secondLoan = LoanCreateRequest.builder()
                .borrowerName("Charlie")
                .borrowerEmail("charlie@example.com")
                .dueDate(LocalDate.now().plusWeeks(1))
                .build();

        ResponseEntity<Map> errorResponse = restTemplate.postForEntity(
                "/api/books/{bookId}/loans", secondLoan, Map.class, bookId);
        assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorResponse.getBody().get("message").toString()).contains("No copies available");
    }
}
