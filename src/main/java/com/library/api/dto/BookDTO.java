package com.library.api.dto;

import java.time.LocalDate;

public class BookDTO {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private LocalDate publishedDate;
    private String genre;
    private Integer totalCopies;
    private Integer availableCopies;
    private Integer activeLoansCount;

    public BookDTO() {
    }

    public BookDTO(Long id, String title, String author, String isbn, LocalDate publishedDate,
                   String genre, Integer totalCopies, Integer availableCopies, Integer activeLoansCount) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publishedDate = publishedDate;
        this.genre = genre;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.activeLoansCount = activeLoansCount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public LocalDate getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(LocalDate publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(Integer totalCopies) {
        this.totalCopies = totalCopies;
    }

    public Integer getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(Integer availableCopies) {
        this.availableCopies = availableCopies;
    }

    public Integer getActiveLoansCount() {
        return activeLoansCount;
    }

    public void setActiveLoansCount(Integer activeLoansCount) {
        this.activeLoansCount = activeLoansCount;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String title;
        private String author;
        private String isbn;
        private LocalDate publishedDate;
        private String genre;
        private Integer totalCopies;
        private Integer availableCopies;
        private Integer activeLoansCount;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder isbn(String isbn) {
            this.isbn = isbn;
            return this;
        }

        public Builder publishedDate(LocalDate publishedDate) {
            this.publishedDate = publishedDate;
            return this;
        }

        public Builder genre(String genre) {
            this.genre = genre;
            return this;
        }

        public Builder totalCopies(Integer totalCopies) {
            this.totalCopies = totalCopies;
            return this;
        }

        public Builder availableCopies(Integer availableCopies) {
            this.availableCopies = availableCopies;
            return this;
        }

        public Builder activeLoansCount(Integer activeLoansCount) {
            this.activeLoansCount = activeLoansCount;
            return this;
        }

        public BookDTO build() {
            return new BookDTO(id, title, author, isbn, publishedDate, genre, totalCopies, availableCopies, activeLoansCount);
        }
    }
}
