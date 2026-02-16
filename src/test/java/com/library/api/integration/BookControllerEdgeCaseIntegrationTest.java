package com.library.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.api.dto.BookCreateRequest;
import com.library.api.entity.Book;
import com.library.api.repository.BookRepository;
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
@DisplayName("BookController Edge Case Integration Tests")
class BookControllerEdgeCaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    private Book testBook;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();

        testBook = Book.builder()
                .title("Edge Case Book")
                .author("Test Author")
                .isbn("9781111111111")
                .publishedDate(LocalDate.of(2023, 6, 15))
                .genre("Fiction")
                .totalCopies(3)
                .availableCopies(3)
                .loans(new ArrayList<>())
                .build();
        testBook = bookRepository.save(testBook);
    }

    @Nested
    @DisplayName("Validation Edge Cases")
    class ValidationEdgeCases {

        @Test
        @DisplayName("should return 400 when title is null")
        void shouldReturn400WhenTitleNull() throws Exception {
            String json = "{\"author\":\"Author\",\"isbn\":\"978-0000000001\",\"totalCopies\":1}";

            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors").exists());
        }

        @Test
        @DisplayName("should return 400 when totalCopies is zero")
        void shouldReturn400WhenTotalCopiesZero() throws Exception {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("Valid Title")
                    .author("Valid Author")
                    .isbn("9780000000099")
                    .totalCopies(0)
                    .build();

            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.totalCopies").exists());
        }

        @Test
        @DisplayName("should return 400 when ISBN format is invalid")
        void shouldReturn400WhenIsbnInvalid() throws Exception {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("Valid Title")
                    .author("Valid Author")
                    .isbn("INVALID-ISBN")
                    .totalCopies(1)
                    .build();

            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 with multiple validation errors simultaneously")
        void shouldReturnMultipleValidationErrors() throws Exception {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("")
                    .author("")
                    .isbn("bad")
                    .totalCopies(0)
                    .build();

            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors").exists())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("404 Error Path Edge Cases")
    class NotFoundEdgeCases {

        @Test
        @DisplayName("should return 404 with structured error for GET non-existent book")
        void shouldReturn404ForGetNonExistent() throws Exception {
            mockMvc.perform(get("/api/books/{id}", 99999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("should return 404 for PUT on non-existent book")
        void shouldReturn404ForPutNonExistent() throws Exception {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("Update")
                    .author("Author")
                    .isbn("9780000000001")
                    .totalCopies(1)
                    .build();

            mockMvc.perform(put("/api/books/{id}", 99999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 for DELETE on non-existent book")
        void shouldReturn404ForDeleteNonExistent() throws Exception {
            mockMvc.perform(delete("/api/books/{id}", 99999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Pagination Edge Cases")
    class PaginationEdgeCases {

        @Test
        @DisplayName("should return empty page when requesting beyond total pages")
        void shouldReturnEmptyForBeyondTotalPages() throws Exception {
            mockMvc.perform(get("/api/books")
                            .param("page", "100")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should handle page size of 1")
        void shouldHandlePageSizeOne() throws Exception {
            // Add a second book
            Book book2 = Book.builder()
                    .title("Second Book")
                    .author("Author 2")
                    .isbn("9782222222222")
                    .totalCopies(1)
                    .availableCopies(1)
                    .loans(new ArrayList<>())
                    .build();
            bookRepository.save(book2);

            mockMvc.perform(get("/api/books")
                            .param("page", "1")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        @DisplayName("should use default pagination when no params provided")
        void shouldUseDefaultPagination() throws Exception {
            mockMvc.perform(get("/api/books"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.pageable").exists())
                    .andExpect(jsonPath("$.totalElements").isNumber());
        }

        @Test
        @DisplayName("should sort books by title ascending")
        void shouldSortByTitleAscending() throws Exception {
            Book aBook = Book.builder()
                    .title("Alpha Book")
                    .author("Author")
                    .isbn("9783333333333")
                    .totalCopies(1)
                    .availableCopies(1)
                    .loans(new ArrayList<>())
                    .build();
            bookRepository.save(aBook);

            mockMvc.perform(get("/api/books")
                            .param("sort", "title,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Alpha Book"));
        }
    }

    @Nested
    @DisplayName("Business Logic Edge Cases")
    class BusinessLogicEdgeCases {

        @Test
        @DisplayName("should return 400 when creating book with duplicate ISBN")
        void shouldReturn400ForDuplicateIsbn() throws Exception {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("Another Book")
                    .author("Another Author")
                    .isbn("9781111111111")
                    .totalCopies(1)
                    .build();

            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("already exists")));
        }

        @Test
        @DisplayName("should allow update with unchanged ISBN")
        void shouldAllowUpdateUnchangedIsbn() throws Exception {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("Updated Title")
                    .author("Updated Author")
                    .isbn("9781111111111")
                    .publishedDate(LocalDate.of(2023, 6, 15))
                    .genre("Drama")
                    .totalCopies(10)
                    .build();

            mockMvc.perform(put("/api/books/{id}", testBook.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Title"));
        }
    }
}
