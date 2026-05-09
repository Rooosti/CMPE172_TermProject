# Final Project Report: Online Appointment Scheduling System (Summaries)

*Use these summaries as a foundation for your 5-10 page report. Reword and expand as needed.*

---

## 1. System Overview
The Online Appointment Scheduling System is an enterprise-grade equipment rental platform built with Spring Boot. It allows users to register as either renters or providers (or both). Renters can browse available equipment, book time slots, and communicate with providers. Providers can list equipment, manage availability, and track incoming bookings. The system emphasizes data integrity, preventing double-bookings through advanced concurrency control, and maintains clear architectural boundaries.

---

## 2. Architecture Diagram (Description)
The system follows a strict **Layered Enterprise Architecture**:
- **Presentation Layer (WebController):** Handles HTTP requests, session management, and returns Thymeleaf-rendered views.
- **Service Layer (BookingService, UserService):** Encapsulates business logic, transaction boundaries, and integrations.
- **Data Access Layer (JdbcRepository Implementations):** Uses the **Data Mapper** pattern with `JdbcTemplate` to map SQL rows to Domain POJOs.
- **Database Layer (MySQL):** Relational storage with strict foreign key constraints.
- **External Integration (CalendarIntegrationService):** Conceptual REST boundary for syncing appointments with external calendars.

---

## 3. DB Schema and Rationale
The database is designed with normalized tables to ensure data consistency:
- **User/Role (Many-to-Many):** Decouples user identity from their permissions (Admin, Renter, Provider).
- **Inheritance (Renter/Provider):** Uses the "Table per Type" strategy where specialized attributes (if any) are stored in child tables linked via foreign keys to the main `user` table.
- **Equipment/Booking:** Establishes a clear relationship between assets and their scheduled usage.
- **Rationale:** We avoided ORMs (like Hibernate) to demonstrate a deep understanding of manual SQL mapping. This ensures complete control over query performance and transaction isolation levels.

---

## 4. Concurrency & Transaction Design
To prevent "Double-Booking," we implemented a robust concurrency strategy:
- **Isolation Level:** We use `SERIALIZABLE` isolation for the `bookEquipment` method. This prevents "Phantom Reads" and ensures that the availability check and insertion are atomic.
- **Algorithm:** Overlaps are detected using the logic `(StartA < EndB AND EndA > StartB)`.
- **Resilience:** Because `SERIALIZABLE` transactions can be aborted by the DB during conflicts, we implemented **Spring Retry** with exponential backoff. This allows the system to automatically recover from serialization failures without user intervention.

---

## 5. Distribution Boundary Design
The system demonstrates a "Coarse-Grained" distribution boundary through the `CalendarIntegrationService`. 
- **Pattern:** When a booking is confirmed, the system triggers a conceptual REST call to an external Google Calendar API.
- **Design:** Instead of sending raw database IDs, we send a complete "Event Payload" (Summary, Description, ISO Start/End times). This minimizes "chattiness" across the network and ensures the external service has all the context it needs in a single request.

---

## 6. System Management & Logging
Observability is built into the core of the application:
- **Logging:** We use SLF4J to log every booking attempt, success, and failure. This creates a clear audit trail for debugging and business analysis.
- **Metrics:** Using **Micrometer**, we track the latency of the booking operation (`@Timed`). This helps identify performance bottlenecks in the transaction logic.
- **Health Checks:** We exposed a `/health` endpoint (via Spring Boot Actuator) that provides real-time status of the application and its database connection, crucial for production monitoring.

---

## 7. Limitations & Future Improvements
- **Current Limitations:** The mock remote service is conceptual (logging only); the UI uses basic Tailwind without a frontend framework like React; search functionality is limited to listing all items.
- **Future Improvements:** Implement actual OAuth2 integration with Google Calendar; add a search/filter engine (e.g., Elasticsearch); implement a notification service (SMS/Email) using a similar distribution boundary pattern.
