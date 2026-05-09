# Milestone 6: System Management & Logging Plan

## 1. Logging Plan
Our system uses SLF4J with Logback (default Spring Boot) to track critical events and errors.

### What to Log (Key Events/Errors)
- **Booking Attempts:** Log every request to `bookEquipment` with renter ID, equipment ID, and time range.
- **Booking Success:** Log confirmed bookings with generated IDs and final price.
- **Booking Rejections:** Log when a booking is denied due to concurrency/overlap conflicts.
- **System Failures:** Log stack traces for unhandled exceptions and database connection issues.
- **Distribution Boundary:** Log conceptual REST calls to external services (e.g., Google Calendar).

### Example Log Messages
- **INFO:** `>>> [BOOKING_ATTEMPT] User 2 attempting to rent Equipment 1: 2026-05-10T10:00 - 2026-05-10T12:00`
- **INFO:** `>>> [BOOKING_SUCCESS] Booking ID: 42. Status: CONFIRMED. Total Price: $50.00`
- **WARN:** `>>> [BOOKING_REJECTED] Conflict detected for Equipment 1 at 2026-05-10T10:00`
- **ERROR:** `>>> [BOOKING_FAILURE] Reason: Equipment not found. Data: Booking(renterId=2, equipmentId=999, ...)`
- **INFO:** `>>> [DISTRIBUTION BOUNDARY] Conceptual REST POST to: https://www.googleapis.com/calendar/v3/...`

---

## 2. Monitoring Metrics
We use **Micrometer** and **Spring Boot Actuator** to collect system metrics.

### Key Metrics Tracked
- **Booking Operation Latency:** Tracked via `@Timed(value = "booking.operation.duration")` in `BookingService`.
- **System Health:** Monitored via the `/health` endpoint (redirected to `/actuator/health`).
- **JVM/HTTP Metrics:** Standard metrics exposed via `/actuator/metrics`.

---

## 3. Failure Scenario + Recovery Strategy
### Scenario: Database Serialization Failure
**Description:** Under high concurrent load, two users might try to book the same item at the exact same millisecond. Since we use `SERIALIZABLE` isolation, the MySQL database might detect a deadlock or serialization conflict and kill one of the transactions to preserve data integrity.

**Failure:** One user receives a `TransientDataAccessException` or `SQLException` (Deadlock found when trying to get lock).

**Recovery Strategy:** 
1. **Automated Retry:** We use Spring Retry (`@Retryable`). The system will automatically catch the serialization exception and re-run the `bookEquipment` method up to 3 times.
2. **Exponential Backoff:** The retries are spaced out (100ms, 200ms, 400ms) to allow the competing transaction to finish, reducing further contention.
3. **User Feedback:** If all 3 retries fail (highly unlikely for this use case), the system returns a clear error message to the user, who can then try again manually.

---

## 4. Health Check Endpoint
- **Endpoint:** `GET /health` (Redirects to `/actuator/health`)
- **Returns:** JSON object indicating status of the application and the MySQL database.
- **Example Response:**
  ```json
  {
    "status": "UP",
    "components": {
      "db": {
        "status": "UP",
        "details": { "database": "MySQL", "validationQuery": "isValid()" }
      },
      "ping": { "status": "UP" }
    }
  }
  ```
