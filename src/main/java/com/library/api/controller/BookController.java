package com.library.api.controller;

import com.library.api.dto.BookCreateRequest;
import com.library.api.dto.BookDTO;
import com.library.api.dto.LoanDTO;
import com.library.api.service.BookService;
import com.library.api.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
public class BookController {

    // commit for rehearsal

    private final BookService bookService;
    private final LoanService loanService;

    public BookController(BookService bookService, LoanService loanService) {
        this.bookService = bookService;
        this.loanService = loanService;
    }

    @GetMapping
    public ResponseEntity<Page<BookDTO>> getAllBooks(
            @PageableDefault(size = 10, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(bookService.getAllBooks(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<BookDTO>> searchBooks(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String genre,
            @PageableDefault(size = 10, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(bookService.searchBooks(author, genre, pageable));
    }

    @GetMapping("/available")
    public ResponseEntity<Page<BookDTO>> getAvailableBooks(
            @PageableDefault(size = 10, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(bookService.getAvailableBooks(pageable));
    }

    @PostMapping
    public ResponseEntity<BookDTO> createBook(@Valid @RequestBody BookCreateRequest request) {
        BookDTO createdBook = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBook);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDTO> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookCreateRequest request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/loans")
    public ResponseEntity<Page<LoanDTO>> getLoansForBook(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "loanDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(loanService.getLoansForBook(id, pageable));
    }
}
