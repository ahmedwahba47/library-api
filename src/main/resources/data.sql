-- Sample Books
INSERT INTO books (title, author, isbn, published_date, genre, total_copies, available_copies) VALUES
('The Great Gatsby', 'F. Scott Fitzgerald', '978-0743273565', '1925-04-10', 'Fiction', 5, 4),
('To Kill a Mockingbird', 'Harper Lee', '978-0446310789', '1960-07-11', 'Fiction', 3, 2),
('1984', 'George Orwell', '978-0451524935', '1949-06-08', 'Dystopian', 4, 4),
('Pride and Prejudice', 'Jane Austen', '978-0141439518', '1813-01-28', 'Romance', 3, 3),
('The Catcher in the Rye', 'J.D. Salinger', '978-0316769488', '1951-07-16', 'Fiction', 2, 1),
('The Hobbit', 'J.R.R. Tolkien', '978-0547928227', '1937-09-21', 'Fantasy', 6, 5),
('Harry Potter and the Philosopher''s Stone', 'J.K. Rowling', '978-0747532699', '1997-06-26', 'Fantasy', 8, 7),
('The Lord of the Rings', 'J.R.R. Tolkien', '978-0618640157', '1954-07-29', 'Fantasy', 4, 4),
('Animal Farm', 'George Orwell', '978-0451526342', '1945-08-17', 'Satire', 3, 2),
('Brave New World', 'Aldous Huxley', '978-0060850524', '1932-01-01', 'Dystopian', 3, 3);

-- Sample Loans
INSERT INTO loans (book_id, borrower_name, borrower_email, loan_date, due_date, return_date, status) VALUES
(1, 'John Smith', 'john.smith@email.com', '2024-01-15', '2024-02-15', NULL, 'ACTIVE'),
(2, 'Jane Doe', 'jane.doe@email.com', '2024-01-10', '2024-02-10', NULL, 'ACTIVE'),
(3, 'Bob Wilson', 'bob.wilson@email.com', '2024-01-05', '2024-02-05', '2024-01-25', 'RETURNED'),
(5, 'Alice Brown', 'alice.brown@email.com', '2024-01-20', '2024-02-20', NULL, 'ACTIVE'),
(6, 'Charlie Davis', 'charlie.davis@email.com', '2024-01-08', '2024-02-08', NULL, 'ACTIVE'),
(7, 'Emma Johnson', 'emma.johnson@email.com', '2024-01-12', '2024-02-12', NULL, 'ACTIVE'),
(7, 'David Lee', 'david.lee@email.com', '2024-01-18', '2024-02-18', '2024-02-01', 'RETURNED'),
(9, 'Sarah Miller', 'sarah.miller@email.com', '2024-01-03', '2024-02-03', NULL, 'ACTIVE');
