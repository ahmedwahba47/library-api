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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Unit Tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookService bookService;

    private Book testBook;
    private BookDTO testBookDTO;
    private BookCreateRequest testBookRequest;

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

        testBookDTO = BookDTO.builder()
                .id(1L)
                .title("Test Book")
                .author("Test Author")
                .isbn("978-0000000001")
                .publishedDate(LocalDate.of(2023, 1, 1))
                .genre("Fiction")
                .totalCopies(5)
                .availableCopies(5)
                .activeLoansCount(0)
                .build();

        testBookRequest = BookCreateRequest.builder()
                .title("Test Book")
                .author("Test Author")
                .isbn("978-0000000001")
                .publishedDate(LocalDate.of(2023, 1, 1))
                .genre("Fiction")
                .totalCopies(5)
                .build();
    }

    @Nested
    @DisplayName("getAllBooks")
    class GetAllBooks {

        @Test
        @DisplayName("should return paginated books")
        void shouldReturnPaginatedBooks() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> bookPage = new PageImpl<>(List.of(testBook));

            when(bookRepository.findAll(pageable)).thenReturn(bookPage);
            when(bookMapper.toDTO(testBook)).thenReturn(testBookDTO);

            Page<BookDTO> result = bookService.getAllBooks(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Wrong Book");
            verify(bookRepository).findAll(pageable);
        }

        @Test
        @DisplayName("should return empty page when no books exist")
        void shouldReturnEmptyPageWhenNoBooksExist() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> emptyPage = new PageImpl<>(List.of());

            when(bookRepository.findAll(pageable)).thenReturn(emptyPage);

            Page<BookDTO> result = bookService.getAllBooks(pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBookById")
    class GetBookById {

        @Test
        @DisplayName("should return book when found")
        void shouldReturnBookWhenFound() {
            when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
            when(bookMapper.toDTO(testBook)).thenReturn(testBookDTO);

            BookDTO result = bookService.getBookById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Test Book");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when book not found")
        void shouldThrowExceptionWhenBookNotFound() {
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.getBookById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Book not found with id: 999");
        }
    }

    @Nested
    @DisplayName("createBook")
    class CreateBook {

        @Test
        @DisplayName("should create book successfully")
        void shouldCreateBookSuccessfully() {
            when(bookRepository.existsByIsbn(testBookRequest.getIsbn())).thenReturn(false);
            when(bookMapper.toEntity(testBookRequest)).thenReturn(testBook);
            when(bookRepository.save(testBook)).thenReturn(testBook);
            when(bookMapper.toDTO(testBook)).thenReturn(testBookDTO);

            BookDTO result = bookService.createBook(testBookRequest);

            assertThat(result.getTitle()).isEqualTo("Test Book");
            verify(bookRepository).save(testBook);
        }

        @Test
        @DisplayName("should throw BusinessException when ISBN already exists")
        void shouldThrowExceptionWhenIsbnExists() {
            when(bookRepository.existsByIsbn(testBookRequest.getIsbn())).thenReturn(true);

            assertThatThrownBy(() -> bookService.createBook(testBookRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("updateBook")
    class UpdateBook {

        @Test
        @DisplayName("should update book successfully")
        void shouldUpdateBookSuccessfully() {
            // Set different ISBN to trigger the existsByIsbn check
            testBookRequest.setIsbn("978-0000000002");
            when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
            when(bookRepository.existsByIsbn("978-0000000002")).thenReturn(false);
            when(bookRepository.save(testBook)).thenReturn(testBook);
            when(bookMapper.toDTO(testBook)).thenReturn(testBookDTO);

            BookDTO result = bookService.updateBook(1L, testBookRequest);

            assertThat(result).isNotNull();
            verify(bookMapper).updateEntity(testBook, testBookRequest);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when book not found")
        void shouldThrowExceptionWhenBookNotFound() {
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.updateBook(999L, testBookRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteBook")
    class DeleteBook {

        @Test
        @DisplayName("should delete book successfully")
        void shouldDeleteBookSuccessfully() {
            when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
            doNothing().when(bookRepository).delete(testBook);

            bookService.deleteBook(1L);

            verify(bookRepository).delete(testBook);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when book not found")
        void shouldThrowExceptionWhenBookNotFound() {
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.deleteBook(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("searchBooks")
    class SearchBooks {

        @Test
        @DisplayName("should search books by author and genre")
        void shouldSearchBooksByAuthorAndGenre() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> bookPage = new PageImpl<>(List.of(testBook));

            when(bookRepository.searchBooks("Test Author", "Fiction", pageable)).thenReturn(bookPage);
            when(bookMapper.toDTO(testBook)).thenReturn(testBookDTO);

            Page<BookDTO> result = bookService.searchBooks("Test Author", "Fiction", pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("availableCopies management")
    class AvailableCopiesManagement {

        @Test
        @DisplayName("should decrement available copies")
        void shouldDecrementAvailableCopies() {
            testBook.setAvailableCopies(5);
            when(bookRepository.save(testBook)).thenReturn(testBook);

            bookService.decrementAvailableCopies(testBook);

            assertThat(testBook.getAvailableCopies()).isEqualTo(4);
        }

        @Test
        @DisplayName("should throw BusinessException when no copies available")
        void shouldThrowExceptionWhenNoCopiesAvailable() {
            testBook.setAvailableCopies(0);

            assertThatThrownBy(() -> bookService.decrementAvailableCopies(testBook))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No copies available");
        }

        @Test
        @DisplayName("should increment available copies")
        void shouldIncrementAvailableCopies() {
            testBook.setAvailableCopies(4);
            when(bookRepository.save(testBook)).thenReturn(testBook);

            bookService.incrementAvailableCopies(testBook);

            assertThat(testBook.getAvailableCopies()).isEqualTo(5);
        }

        @Test
        @DisplayName("should throw BusinessException when all copies already returned")
        void shouldThrowExceptionWhenAllCopiesReturned() {
            testBook.setAvailableCopies(5);
            testBook.setTotalCopies(5);

            assertThatThrownBy(() -> bookService.incrementAvailableCopies(testBook))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("All copies already returned");
        }
    }
}
