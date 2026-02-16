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
@DisplayName("BookController Integration Tests")
class BookControllerIntegrationTest {

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
                .title("Integration Test Book")
                .author("Test Author")
                .isbn("9781234567890")
                .publishedDate(LocalDate.of(2023, 6, 15))
                .genre("Fiction")
                .totalCopies(5)
                .availableCopies(5)
                .loans(new ArrayList<>())
                .build();
        testBook = bookRepository.save(testBook);
    }

    @Nested
    @DisplayName("GET /api/books")
    class GetAllBooks {

        @Test
        @DisplayName("should return paginated list of books")
        void shouldReturnPaginatedBooks() throws Exception {
            mockMvc.perform(get("/api/books")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].title").value("Integration Test Book"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("should return empty page when no books")
        void shouldReturnEmptyPage() throws Exception {
            bookRepository.deleteAll();

            mockMvc.perform(get("/api/books"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/books/{id}")
    class GetBookById {

        @Test
        @DisplayName("should return book when found")
        void shouldReturnBookWhenFound() throws Exception {
            mockMvc.perform(get("/api/books/{id}", testBook.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testBook.getId()))
                    .andExpect(jsonPath("$.title").value("Integration Test Book"))
                    .andExpect(jsonPath("$.author").value("Test Author"))
                    .andExpect(jsonPath("$.isbn").value("9781234567890"));
        }

        @Test
        @DisplayName("should return 404 when book not found")
        void shouldReturn404WhenNotFound() throws Exception {
            mockMvc.perform(get("/api/books/{id}", 99999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value(containsString("Book not found")));
        }
    }

    @Nested
    @DisplayName("GET /api/books/search")
    class SearchBooks {

        @Test
        @DisplayName("should filter books by author")
        void shouldFilterByAuthor() throws Exception {
            mockMvc.perform(get("/api/books/search")
                            .param("author", "Test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].author").value("Test Author"));
        }

        @Test
        @DisplayName("should filter books by genre")
        void shouldFilterByGenre() throws Exception {
            mockMvc.perform(get("/api/books/search")
                            .param("genre", "Fiction"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        @DisplayName("should filter books by author and genre")
        void shouldFilterByAuthorAndGenre() throws Exception {
            mockMvc.perform(get("/api/books/search")
                            .param("author", "Test")
                            .param("genre", "Fiction"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("POST /api/books")
    class CreateBook {

        @Test
        @DisplayName("should create book with valid request")
        void shouldCreateBookWithValidRequest() throws Exception {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("New Book")
                    .author("New Author")
                    .isbn("9780987654321")
                    .publishedDate(LocalDate.of(2023, 1, 1))
                    .genre("Science Fiction")
                    .totalCopies(3)
                    .build();

            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("New Book"))
                    .andExpect(jsonPath("$.author").value("New Author"))
                    .andExpect(jsonPath("$.availableCopies").value(3));
        }

        @Test
        @DisplayName("should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("")
                    .author("")
                    .isbn("invalid")
                    .totalCopies(0)
                    .build();

            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.validationErrors").exists());
        }

        @Test
        @DisplayName("should return 400 for duplicate ISBN")
        void shouldReturn400ForDuplicateIsbn() throws Exception {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("Duplicate Book")
                    .author("Some Author")
                    .isbn("9781234567890")
                    .totalCopies(2)
                    .build();

            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("already exists")));
        }
    }

    @Nested
    @DisplayName("PUT /api/books/{id}")
    class UpdateBook {

        @Test
        @DisplayName("should update book with valid request")
        void shouldUpdateBookWithValidRequest() throws Exception {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("Updated Title")
                    .author("Updated Author")
                    .isbn("9781234567890")
                    .publishedDate(LocalDate.of(2023, 6, 15))
                    .genre("Drama")
                    .totalCopies(10)
                    .build();

            mockMvc.perform(put("/api/books/{id}", testBook.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Title"))
                    .andExpect(jsonPath("$.author").value("Updated Author"))
                    .andExpect(jsonPath("$.genre").value("Drama"));
        }

        @Test
        @DisplayName("should return 404 when book not found")
        void shouldReturn404WhenNotFound() throws Exception {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("Updated Title")
                    .author("Updated Author")
                    .isbn("9781111111111")
                    .totalCopies(5)
                    .build();

            mockMvc.perform(put("/api/books/{id}", 99999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/books/{id}")
    class DeleteBook {

        @Test
        @DisplayName("should delete book successfully")
        void shouldDeleteBookSuccessfully() throws Exception {
            mockMvc.perform(delete("/api/books/{id}", testBook.getId()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/books/{id}", testBook.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 when book not found")
        void shouldReturn404WhenNotFound() throws Exception {
            mockMvc.perform(delete("/api/books/{id}", 99999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Pagination")
    class PaginationTests {

        @BeforeEach
        void setUpMultipleBooks() {
            for (int i = 2; i <= 15; i++) {
                Book book = Book.builder()
                        .title("Book " + i)
                        .author("Author " + i)
                        .isbn("978000000000" + i)
                        .genre("Genre")
                        .totalCopies(1)
                        .availableCopies(1)
                        .loans(new ArrayList<>())
                        .build();
                bookRepository.save(book);
            }
        }

        @Test
        @DisplayName("should paginate correctly with custom page size")
        void shouldPaginateCorrectly() throws Exception {
            mockMvc.perform(get("/api/books")
                            .param("page", "1")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(5)))
                    .andExpect(jsonPath("$.totalElements").value(15))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andExpect(jsonPath("$.number").value(0));
        }

        @Test
        @DisplayName("should return correct page number")
        void shouldReturnCorrectPage() throws Exception {
            mockMvc.perform(get("/api/books")
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(5)))
                    .andExpect(jsonPath("$.number").value(1));
        }
    }
}
