# 🏦 AgoraBank

![Java](https://img.shields.io/badge/Java-25-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen)
![Build](https://img.shields.io/badge/build-passing-success)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

AgoraBank is a backend banking application built with **Spring Boot** that simulates core banking features such as user authentication, account management, transactions, roles, and idempotency handling.

This project is designed as a clean, modular, and test-driven system following best practices in modern Java backend development.

---

## 🚀 Features

### 👤 Authentication & Authorization
- User registration and authentication
- Role-based access control (e.g., `USER`, `ADMIN`)
- Secure password storage (hashed passwords)
- Account enable/disable support

### 🏦 Accounts
- Create and manage bank accounts
- Account status management (`ACTIVE`, `BLOCKED`, `CLOSED`)
- Multiple account types and currencies
- Customer–Account relationship

### 💸 Transactions
- Deposits, withdrawals, and transfers
- Transaction status tracking
- Pagination and filtering (by account, status, date, etc.)
- Balance tracking after operations

### 🔁 Idempotency Handling
- Prevents duplicate transaction processing
- Scope-based idempotency records
- Expiration management and cleanup
- Duplicate detection by account + scope + key + status

---

## 🧱 Architecture

The application follows a layered architecture:

- **Controller Layer** – REST endpoints
- **Service Layer** – Business logic
- **Repository Layer** – Spring Data JPA repositories
- **Domain Model** – JPA entities with enums and relationships

The project uses:

- Spring Boot
- Spring Data JPA
- Hibernate
- H2 (for testing)
- JUnit 5
- AssertJ

---

## 🧪 Testing Strategy

Testing is a core part of the project and follows a clean, structured, and maintainable approach.

### 🔍 Repository Layer Testing

The persistence layer is tested using `@DataJpaTest`, focusing on:

- Query derivation methods
- Combined filters (e.g., username + enabled, account + status)
- Existence checks (`existsBy...`)
- Idempotency duplicate validation logic
- Status-based and boolean filtering

Each test follows a clear and consistent structure:

```java
// given  -> test data setup
// when   -> repository method execution
// then   -> assertions
```

This structure ensures readability, clarity of intent, and long-term maintainability.

Tests cover:
- Query methods
- Filters and combinations
- Existence checks
- Idempotency duplicate detection
- Enabled/disabled user filtering

### 🛠 Testing Tools

- JUnit 5
- AssertJ (fluent assertions)
- H2 in-memory database
- Spring Boot Test support

---

## 📚 Goals of the Project

This project aims to:

- Demonstrate clean domain modeling
- Apply strong typing with enums instead of raw strings
- Practice repository query derivation in Spring Data
- Write readable, structured unit tests
- Simulate real banking domain constraints

---

## 🛠 Future Improvements

- JWT authentication
- API documentation with OpenAPI/Swagger
- Integration tests
- Docker support
- Production database profile

---

## ⚠️ Disclaimer

This is a learning/demo banking system and is **not intended for production use**.

---

Made with ☕ and Spring Boot.
