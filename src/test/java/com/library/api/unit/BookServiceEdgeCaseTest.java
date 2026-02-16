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
    @DisplayName("getBookById edge cases")
    class GetBookByIdEdgeCases {

        @Test
        @DisplayName("should throw ResourceNotFoundException for negative ID")
        void shouldThrowExceptionForNegativeId() {
            when(bookRepository.findById(-1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.getBookById(-1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for zero ID")
        void shouldThrowExceptionForZeroId() {
            when(bookRepository.findById(0L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.getBookById(0L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for Long.MAX_VALUE ID")
        void shouldThrowExceptionForMaxLongId() {
            when(bookRepository.findById(Long.MAX_VALUE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.getBookById(Long.MAX_VALUE))
                    .isInstanceOf(ResourceNotFoundException.class);
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
        @DisplayName("should increment from 0 to 1 (first return)")
        void shouldIncrementFirstReturn() {
            testBook.setAvailableCopies(0);
            testBook.setTotalCopies(5);
            when(bookRepository.save(testBook)).thenReturn(testBook);

            bookService.incrementAvailableCopies(testBook);

            assertThat(testBook.getAvailableCopies()).isEqualTo(1);
        }

        @Test
        @DisplayName("should increment to totalCopies minus one (second to last)")
        void shouldIncrementToSecondToLast() {
            testBook.setAvailableCopies(3);
            testBook.setTotalCopies(5);
            when(bookRepository.save(testBook)).thenReturn(testBook);

            bookService.incrementAvailableCopies(testBook);

            assertThat(testBook.getAvailableCopies()).isEqualTo(4);
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

    @Nested
    @DisplayName("pagination edge cases")
    class PaginationEdgeCases {

        @Test
        @DisplayName("should handle large page size gracefully")
        void shouldHandleLargePageSize() {
            Pageable pageable = PageRequest.of(0, 1000);
            Page<Book> bookPage = new PageImpl<>(List.of(testBook), pageable, 1);
            BookDTO dto = BookDTO.builder().id(1L).title("Test Book").build();

            when(bookRepository.findAll(pageable)).thenReturn(bookPage);
            when(bookMapper.toDTO(testBook)).thenReturn(dto);

            Page<BookDTO> result = bookService.getAllBooks(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty page for out-of-range page number")
        void shouldReturnEmptyPageForOutOfRange() {
            Pageable pageable = PageRequest.of(100, 10);
            Page<Book> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(bookRepository.findAll(pageable)).thenReturn(emptyPage);

            Page<BookDTO> result = bookService.getAllBooks(pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("search edge cases")
    class SearchEdgeCases {

        @Test
        @DisplayName("should return empty results when no match found")
        void shouldReturnEmptyWhenNoMatch() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> emptyPage = new PageImpl<>(List.of());

            when(bookRepository.searchBooks("NonExistentAuthor", "NonExistentGenre", pageable))
                    .thenReturn(emptyPage);

            Page<BookDTO> result = bookService.searchBooks("NonExistentAuthor", "NonExistentGenre", pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should search with null author parameter")
        void shouldSearchWithNullAuthor() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> bookPage = new PageImpl<>(List.of(testBook));
            BookDTO dto = BookDTO.builder().id(1L).title("Test Book").build();

            when(bookRepository.searchBooks(null, "Fiction", pageable)).thenReturn(bookPage);
            when(bookMapper.toDTO(testBook)).thenReturn(dto);

            Page<BookDTO> result = bookService.searchBooks(null, "Fiction", pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should search with null genre parameter")
        void shouldSearchWithNullGenre() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> bookPage = new PageImpl<>(List.of(testBook));
            BookDTO dto = BookDTO.builder().id(1L).title("Test Book").build();

            when(bookRepository.searchBooks("Test Author", null, pageable)).thenReturn(bookPage);
            when(bookMapper.toDTO(testBook)).thenReturn(dto);

            Page<BookDTO> result = bookService.searchBooks("Test Author", null, pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }
}
