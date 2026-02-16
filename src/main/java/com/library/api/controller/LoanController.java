package com.library.api.controller;

import com.library.api.dto.LoanCreateRequest;
import com.library.api.dto.LoanDTO;
import com.library.api.entity.Loan.LoanStatus;
import com.library.api.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/loans")
    public ResponseEntity<Page<LoanDTO>> getAllLoans(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 10, sort = "loanDate", direction = Sort.Direction.DESC) Pageable pageable) {

        if (status != null || startDate != null || endDate != null) {
            return ResponseEntity.ok(loanService.searchLoans(status, startDate, endDate, pageable));
        }
        return ResponseEntity.ok(loanService.getAllLoans(pageable));
    }

    @GetMapping("/loans/{id}")
    public ResponseEntity<LoanDTO> getLoanById(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getLoanById(id));
    }

    @PostMapping("/books/{bookId}/loans")
    public ResponseEntity<LoanDTO> createLoan(
            @PathVariable Long bookId,
            @Valid @RequestBody LoanCreateRequest request) {
        LoanDTO createdLoan = loanService.createLoan(bookId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLoan);
    }

    @PutMapping("/loans/{id}")
    public ResponseEntity<LoanDTO> updateLoan(
            @PathVariable Long id,
            @Valid @RequestBody LoanCreateRequest request) {
        return ResponseEntity.ok(loanService.updateLoan(id, request));
    }

    @PutMapping("/loans/{id}/return")
    public ResponseEntity<LoanDTO> returnLoan(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.returnLoan(id));
    }

    @DeleteMapping("/loans/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        loanService.deleteLoan(id);
        return ResponseEntity.noContent().build();
    }
}
