# Library Management REST API

A Spring Boot REST API for managing a library's book catalogue and loan operations. Built as part of MSc coursework to demonstrate RESTful API design principles, microservices architecture patterns, and CI/CD pipeline implementation.

## Overview

The Library Management API provides a complete CRUD interface for managing books and their associated loans. It follows a layered architecture pattern (Controller → Service → Repository) with strict separation of concerns, DTO-based request/response handling, and comprehensive input validation.

### Key Features

- **RESTful Design** — Resource-oriented endpoints following Richardson Maturity Model Level 2 (HTTP verbs + resource URIs)
- **Entity Relationships** — One-to-many relationship between Books and Loans with cascade operations
- **DTO Pattern** — Separate request/response objects to decouple API contract from internal entities
- **Pagination & Sorting** — Spring Data pageable responses with configurable page size, sorting by any field
- **Date Filtering** — ISO 8601 date parameters for filtering loans by date range
- **Input Validation** — Jakarta Bean Validation with ISBN format checking, email validation, future date constraints
- **Error Handling** — Consistent error response structure inspired by RFC 7807 Problem Details
- **Available Copies Tracking** — Automatic inventory management when loans are created/returned

## Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Runtime | Java 21 | Application runtime |
| Framework | Spring Boot 3.2.0 | Web framework, dependency injection, auto-configuration |
| Persistence | Spring Data JPA | Repository abstraction, query derivation |
| Database (Dev) | H2 | In-memory database for development and testing |
| Database (Prod) | PostgreSQL | Production-grade relational storage |
| Build Tool | Maven 3.9+ | Dependency management, build lifecycle, test execution |
| Containerisation | Docker | Multi-stage builds, environment-independent deployment |
| CI/CD | Jenkins | Declarative pipeline with 9 automated stages |
| Code Quality | SonarQube | Static analysis, coverage reporting, quality gates |
| Testing | JUnit 5, Mockito, Spring MockMvc | Unit and integration test frameworks |

## Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Controller  │────▶│   Service    │────▶│  Repository  │────▶│   Database   │
│   (HTTP)     │     │  (Business)  │     │    (JPA)     │     │   (H2/PG)    │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
       │                    │
       ▼                    ▼
┌──────────────┐     ┌──────────────┐
│     DTOs     │     │   Mappers    │
│ (Validation) │     │ (Entity↔DTO) │
└──────────────┘     └──────────────┘
```

- **Controllers** handle HTTP request/response mapping and delegate to services
- **Services** contain business logic, transactional boundaries, and validation rules
- **Repositories** extend Spring Data JPA interfaces for data access
- **DTOs** define the API contract with Jakarta validation annotations
- **Mappers** convert between entities and DTOs, keeping layers decoupled

## Project Structure

```
library-api/
├── src/main/java/com/library/api/
│   ├── controller/          # REST endpoints (BookController, LoanController)
│   ├── service/             # Business logic (BookService, LoanService)
│   ├── repository/          # Data access (BookRepository, LoanRepository)
│   ├── entity/              # JPA entities (Book, Loan)
│   ├── dto/                 # Request/response objects with validation
│   ├── mapper/              # Entity ↔ DTO conversion
│   └── exception/           # Global error handling, custom exceptions
├── src/main/resources/
│   ├── application.yml      # Configuration (profiles, datasource, JPA)
│   └── data.sql             # Seed data for development
├── src/test/java/com/library/api/
│   ├── unit/                # Unit tests (Mockito-based, no Spring context)
│   └── integration/         # Integration tests (full Spring context + H2)
├── Dockerfile               # Multi-stage container build
├── Jenkinsfile              # 9-stage CI/CD pipeline definition
├── docker-compose.yml       # Infrastructure (Jenkins, SonarQube, PostgreSQL)
└── pom.xml                  # Maven configuration with test plugins
```

## API Endpoints

### Books (`/api/books`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/books` | List all books (paginated) |
| `GET` | `/api/books/{id}` | Get book by ID |
| `GET` | `/api/books/search?author=X&genre=Y` | Search books by author/genre |
| `GET` | `/api/books/available` | List books with available copies |
| `POST` | `/api/books` | Create a new book |
| `PUT` | `/api/books/{id}` | Update an existing book |
| `DELETE` | `/api/books/{id}` | Delete a book (cascades to loans) |
| `GET` | `/api/books/{id}/loans` | Get all loans for a specific book |

### Loans (`/api/loans`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/loans` | List all loans (paginated) |
| `GET` | `/api/loans/{id}` | Get loan by ID |
| `GET` | `/api/loans?status=X&startDate=Y&endDate=Z` | Filter loans by status/date |
| `POST` | `/api/books/{bookId}/loans` | Create a loan for a book |
| `PUT` | `/api/loans/{id}` | Update loan details |
| `PUT` | `/api/loans/{id}/return` | Return a borrowed book |
| `DELETE` | `/api/loans/{id}` | Delete a loan record |

### Pagination

All list endpoints return paginated responses:

```json
{
  "content": [...],
  "totalElements": 25,
  "totalPages": 3,
  "size": 10,
  "number": 0
}
```

Query parameters: `page` (0-indexed), `size` (default 10), `sort` (e.g., `title,asc`).

### Error Responses

All errors follow a consistent structure:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Book with ISBN 9781234567890 already exists",
  "timestamp": "2025-01-15T10:30:00"
}
```

Validation errors include field-level detail:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Input validation failed",
  "validationErrors": {
    "title": "Title is required",
    "totalCopies": "Total copies must be at least 1"
  }
}
```

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+ (or use the included Maven wrapper `./mvnw`)
- Docker (for CI/CD infrastructure and containerised deployment)

### Run Locally

```bash
git clone https://github.com/ahmedwahba47/library-api.git
cd library-api

# Build and run with Maven wrapper
./mvnw spring-boot:run

# Application starts on http://localhost:8080
curl http://localhost:8080/api/books
```

The application starts with H2 in-memory database and seed data by default.

### Run Tests

```bash
# Unit tests only (58 tests, ~3 seconds)
./mvnw test

# Unit + Integration tests (118 tests total)
./mvnw verify

# With coverage report (JaCoCo)
./mvnw verify
# Report at: target/site/jacoco/index.html
```

### Docker

```bash
# Build container image
docker build -t library-api .

# Run container
docker run -p 8080:8080 library-api

# Verify
curl http://localhost:8080/api/books
```

The Dockerfile uses a multi-stage build: JDK for compilation, JRE-only for the runtime image. The application runs as a non-root user for security.

## Testing

The test suite follows the Test Pyramid model:

| Layer | Count | Framework | Scope |
|-------|-------|-----------|-------|
| Unit Tests | 58 | JUnit 5 + Mockito | Service methods in isolation |
| Integration Tests | 60 | Spring MockMvc + H2 | Full HTTP request/response cycle |

**Unit tests** mock repository and mapper dependencies to test business logic in isolation: CRUD operations, ISBN uniqueness, available copies management, boundary values, and error paths.

**Integration tests** load the full Spring context with H2 database and test actual HTTP endpoints: status codes, validation errors, pagination, date filtering, sorting, and cascade operations.

## CI/CD Pipeline

The Jenkinsfile defines a 9-stage declarative pipeline:

```
Checkout → Build → Unit Tests → Integration Tests → SonarQube → Quality Gate → Package → Docker Build → Deploy
```

Each stage must pass before the next begins (fail-fast). The quality gate enforces minimum 70% code coverage, zero critical bugs, and less than 3% duplication.

### Start CI/CD Infrastructure

```bash
# Jenkins (port 8081) + SonarQube (port 9000)
docker-compose up -d jenkins sonarqube sonar-db

# Full stack including the API
docker-compose --profile deploy up -d
```

| Service | URL |
|---------|-----|
| Jenkins | http://localhost:8081 |
| SonarQube | http://localhost:9000 |
| Library API | http://localhost:8080 |

## API Examples

### Create a Book

```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "The Great Gatsby",
    "author": "F. Scott Fitzgerald",
    "isbn": "9780743273565",
    "publishedDate": "1925-04-10",
    "genre": "Fiction",
    "totalCopies": 5
  }'
```

### Create a Loan

```bash
curl -X POST http://localhost:8080/api/books/1/loans \
  -H "Content-Type: application/json" \
  -d '{
    "borrowerName": "John Smith",
    "borrowerEmail": "john@email.com",
    "dueDate": "2026-03-15"
  }'
```

### Return a Book

```bash
curl -X PUT http://localhost:8080/api/loans/1/return
```

### Search with Pagination

```bash
# Page 0, 5 items, sorted by title
curl "http://localhost:8080/api/books?page=0&size=5&sort=title,asc"

# Filter loans by date range
curl "http://localhost:8080/api/loans?startDate=2026-01-01&endDate=2026-12-31"
```

## Configuration

### Development (default profile)

- H2 in-memory database with auto-generated schema
- H2 Console available at http://localhost:8080/h2-console
- Seed data loaded from `data.sql`

### Production

Set the following environment variables:

```bash
SPRING_PROFILES_ACTIVE=prod
DB_HOST=localhost
DB_PORT=5432
DB_NAME=library_db
DB_USERNAME=library_user
DB_PASSWORD=your_password
```

## Licence

This project is for educational purposes as part of MSc coursework at CCT College Dublin.
