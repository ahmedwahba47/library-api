package com.library.api.unit;

import com.library.api.dto.BookCreateRequest;
import com.library.api.dto.BookDTO;
import com.library.api.entity.Book;
import com.library.api.exception.BusinessException;
import com.library.api.exception.ResourceNotFoundException;
import com.library.api.mapper.BookMapper;
import com.library.api.repository.BookRepository;
import com.library.api.service.BookService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Edge Case Tests")
class BookServiceEdgeCaseTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookService bookService;

    private Book testBook;

    @BeforeEach
    void setUp() {
        testBook = Book.builder()
                .id(1L)
                .title("Test Book")
                .author("Test Author")
                .isbn("978-0000000001")
                .publishedDate(LocalDate.of(2023, 1, 1))
                .genre("Fiction")
                .totalCopies(5)
                .availableCopies(5)
                .loans(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("getAvailableBooks")
    class GetAvailableBooks {

        @Test
        @DisplayName("should return only books with available copies")
        void shouldReturnAvailableBooks() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> bookPage = new PageImpl<>(List.of(testBook));
            BookDTO dto = BookDTO.builder().id(1L).title("Test Book").availableCopies(5).build();

            when(bookRepository.findAvailableBooks(pageable)).thenReturn(bookPage);
            when(bookMapper.toDTO(testBook)).thenReturn(dto);

            Page<BookDTO> result = bookService.getAvailableBooks(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAvailableCopies()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should return empty page when no books are available")
        void shouldReturnEmptyWhenNoneAvailable() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> emptyPage = new PageImpl<>(List.of());

            when(bookRepository.findAvailableBooks(pageable)).thenReturn(emptyPage);

            Page<BookDTO> result = bookService.getAvailableBooks(pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchBooks edge cases")
    class SearchBooksEdgeCases {

        @Test
        @DisplayName("should search by author only when genre is null")
        void shouldSearchByAuthorOnly() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> bookPage = new PageImpl<>(List.of(testBook));
            BookDTO dto = BookDTO.builder().id(1L).title("Test Book").author("Test Author").build();

            when(bookRepository.searchBooks("Test Author", null, pageable)).thenReturn(bookPage);
            when(bookMapper.toDTO(testBook)).thenReturn(dto);

            Page<BookDTO> result = bookService.searchBooks("Test Author", null, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(bookRepository).searchBooks("Test Author", null, pageable);
        }
    }

    @Nested
    @DisplayName("createBook edge cases")
    class CreateBookEdgeCases {

        @Test
        @DisplayName("should set availableCopies equal to totalCopies on creation")
        void shouldSetAvailableCopiesEqualToTotalCopies() {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("New Book")
                    .author("Author")
                    .isbn("978-0000000099")
                    .totalCopies(3)
                    .build();

            Book newBook = Book.builder()
                    .id(2L)
                    .title("New Book")
                    .author("Author")
                    .isbn("978-0000000099")
                    .totalCopies(3)
                    .availableCopies(3)
                    .loans(new ArrayList<>())
                    .build();

            BookDTO expectedDTO = BookDTO.builder()
                    .id(2L)
                    .title("New Book")
                    .totalCopies(3)
                    .availableCopies(3)
                    .activeLoansCount(0)
                    .build();

            when(bookRepository.existsByIsbn("978-0000000099")).thenReturn(false);
            when(bookMapper.toEntity(request)).thenReturn(newBook);
            when(bookRepository.save(newBook)).thenReturn(newBook);
            when(bookMapper.toDTO(newBook)).thenReturn(expectedDTO);

            BookDTO result = bookService.createBook(request);

            assertThat(result.getAvailableCopies()).isEqualTo(result.getTotalCopies());
            assertThat(result.getActiveLoansCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should reject duplicate ISBN with exact match")
        void shouldRejectDuplicateIsbnExactMatch() {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("Different Title")
                    .author("Different Author")
                    .isbn("978-0000000001")
                    .totalCopies(1)
                    .build();

            when(bookRepository.existsByIsbn("978-0000000001")).thenReturn(true);

            assertThatThrownBy(() -> bookService.createBook(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("updateBook edge cases")
    class UpdateBookEdgeCases {

        @Test
        @DisplayName("should allow update with same ISBN as current book")
        void shouldAllowUpdateWithSameIsbn() {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("Updated Title")
                    .author("Updated Author")
                    .isbn("978-0000000001")
                    .totalCopies(10)
                    .build();

            BookDTO updatedDTO = BookDTO.builder()
                    .id(1L)
                    .title("Updated Title")
                    .author("Updated Author")
                    .isbn("978-0000000001")
                    .totalCopies(10)
                    .build();

            when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
            when(bookRepository.save(testBook)).thenReturn(testBook);
            when(bookMapper.toDTO(testBook)).thenReturn(updatedDTO);

            BookDTO result = bookService.updateBook(1L, request);

            assertThat(result.getTitle()).isEqualTo("Updated Title");
            verify(bookRepository, never()).existsByIsbn(anyString());
        }

        @Test
        @DisplayName("should reject update with ISBN belonging to another book")
        void shouldRejectUpdateWithExistingIsbn() {
            BookCreateRequest request = BookCreateRequest.builder()
                    .title("Updated Title")
                    .author("Updated Author")
                    .isbn("978-9999999999")
                    .totalCopies(5)
                    .build();

            when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
            when(bookRepository.existsByIsbn("978-9999999999")).thenReturn(true);

            assertThatThrownBy(() -> bookService.updateBook(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("availableCopies boundary conditions")
    class AvailableCopiesBoundary {

        @Test
        @DisplayName("should decrement from 1 to 0 (last copy)")
        void shouldDecrementLastCopy() {
            testBook.setAvailableCopies(1);
            when(bookRepository.save(testBook)).thenReturn(testBook);

            bookService.decrementAvailableCopies(testBook);

            assertThat(testBook.getAvailableCopies()).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw when decrementing from 0")
        void shouldThrowWhenDecrementingFromZero() {
            testBook.setAvailableCopies(0);

            assertThatThrownBy(() -> bookService.decrementAvailableCopies(testBook))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No copies available");

            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when incrementing at totalCopies")
        void shouldThrowWhenIncrementingAtMax() {
            testBook.setAvailableCopies(5);
            testBook.setTotalCopies(5);

            assertThatThrownBy(() -> bookService.incrementAvailableCopies(testBook))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("All copies already returned");

            verify(bookRepository, never()).save(any());
        }
    }
}
