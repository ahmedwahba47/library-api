package com.library.api.service;

import com.library.api.dto.BookCreateRequest;
import com.library.api.dto.BookDTO;
import com.library.api.entity.Book;
import com.library.api.exception.BusinessException;
import com.library.api.exception.ResourceNotFoundException;
import com.library.api.mapper.BookMapper;
import com.library.api.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    @Transactional(readOnly = true)
    public Page<BookDTO> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable)
                .map(bookMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public BookDTO getBookById(Long id) {
        Book book = findBookById(id);
        return bookMapper.toDTO(book);
    }

    @Transactional(readOnly = true)
    public Page<BookDTO> searchBooks(String author, String genre, Pageable pageable) {
        return bookRepository.searchBooks(author, genre, pageable)
                .map(bookMapper::toDTO);
    }

    public BookDTO createBook(BookCreateRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new BusinessException("Book with ISBN " + request.getIsbn() + " already exists");
        }

        Book book = bookMapper.toEntity(request);
        book = bookRepository.save(book);
        return bookMapper.toDTO(book);
    }

    public BookDTO updateBook(Long id, BookCreateRequest request) {
        Book book = findBookById(id);

        if (!book.getIsbn().equals(request.getIsbn()) &&
                bookRepository.existsByIsbn(request.getIsbn())) {
            throw new BusinessException("Book with ISBN " + request.getIsbn() + " already exists");
        }

        bookMapper.updateEntity(book, request);
        book = bookRepository.save(book);
        return bookMapper.toDTO(book);
    }

    public void deleteBook(Long id) {
        Book book = findBookById(id);
        bookRepository.delete(book);
    }

    @Transactional(readOnly = true)
    public Page<BookDTO> getAvailableBooks(Pageable pageable) {
        return bookRepository.findAvailableBooks(pageable)
                .map(bookMapper::toDTO);
    }

    public Book findBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
    }

    public void decrementAvailableCopies(Book book) {
        if (book.getAvailableCopies() <= 0) {
            throw new BusinessException("No copies available for book: " + book.getTitle());
        }
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);
    }

    public void incrementAvailableCopies(Book book) {
        if (book.getAvailableCopies() >= book.getTotalCopies()) {
            throw new BusinessException("All copies already returned for book: " + book.getTitle());
        }
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);
    }
}
