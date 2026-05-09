# Online Appointment Scheduling System

An enterprise-grade, layered Spring Boot application for managing equipment loans and appointments. This project was developed following professional software engineering patterns and strict architectural constraints (No ORM, explicit concurrency control).

## Tech Stack

*   **Backend:** Java 21, Spring Boot 4.0.3
*   **Database:** MySQL 8.0 (Containerized via Docker)
*   **Persistence:** Spring JDBC (`JdbcTemplate`) — *No ORM/Hibernate*
*   **Frontend:** Thymeleaf + Tailwind CSS
*   **Build Tool:** Maven

## Getting Started

### Prerequisites
*   Java 21 installed
*   Docker & Docker Compose installed

### 1. Start the Database
The project uses Docker to provide a clean, isolated MySQL environment.
```bash
docker-compose up -d
```

### 2. Configure Local Environment
The project uses Spring Profiles for security. A template is provided:
1.  Copy `src/main/resources/application.properties.example` to `src/main/resources/application-local.properties`.
2.  The defaults are pre-configured to work with the included `docker-compose.yml`.

### 3. Run the Application
```bash
./mvnw spring-boot:run
```
Visit `http://localhost:8080/login` to access the system.

## Enterprise Architecture & Patterns

This project intentionally avoids JPA/Hibernate to demonstrate a deep understanding of data mapping and manual transaction management.

### 1. Data Mapper Pattern
Instead of letting an ORM handle the database, we use the **Data Mapper** pattern. 
- **Separation of Concerns:** Our Domain Models (`User`, `Equipment`, `Booking`) are pure POJOs with no knowledge of SQL.
- **JdbcTemplate Mapping:** Custom `RowMapper` implementations in the Repository layer translate SQL result sets into Java objects manually, ensuring complete control over the data lifecycle.

### 2. Layered Enterprise Architecture
The system follows a strict unidirectional flow:
`WebController` (View) → `Service` (Business Logic) → `Repository` (Data Access) → `Database`

### 3. Concurrency & Transaction Management
To prevent "Double-Booking" (a core requirement):
- **Atomic Operations:** The `BookingService` uses `@Transactional` to ensure that availability checks and record insertions are atomic.
- **Overlap Logic:** The system uses a specific SQL collision algorithm `(ExistingStart < NewEnd AND ExistingEnd > NewStart)` to prevent conflicting appointments at the database level.

### 4. Domain-Driven Design (Value Objects)
We use **Value Objects** like `Address` to encapsulate complex data types. While the database stores address fields in the `user` table (per the ERD), the Java model groups them into a reusable object, improving code readability and maintainability.

## 📂 Project Structure
- `model/`: Domain entities and value objects.
- `repository/`: Data Mapper interfaces and JDBC implementations.
- `service/`: Business logic and transaction boundaries.
- `controller/`: Web routing and session-based mock authentication.
- `resources/templates/`: Thymeleaf views styled with Tailwind CSS.
- `resources/schema.sql`: Automatic DDL execution on startup.
# CMPE172_TermProject
