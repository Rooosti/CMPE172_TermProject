# Milestone 4: Concurrency Scenario + Transaction Design

## 1. Concrete Race-Condition Scenario
Consider two users, **User A** and **User B**, both viewing the same "Sony A7 III Camera" listing.
1. **User A** selects the time slot 10:00 AM - 12:00 PM and clicks "Book".
2. **User B** selects the exact same time slot 10:00 AM - 12:00 PM and clicks "Book" milliseconds later.
3. Both requests enter the `bookEquipment` method in the `BookingService`.
4. **Thread A** (User A) checks the database: `existsOverlapping` returns `false`.
5. **Thread B** (User B) checks the database: `existsOverlapping` ALSO returns `false` because Thread A hasn't committed its insert yet.
6. **Thread A** proceeds to insert a new booking record.
7. **Thread B** proceeds to insert a new booking record.
8. **Result:** Both users believe they have booked the same equipment for the same time, leading to a "Double-Booking" conflict.

## 2. Applying ACID Principles
To prevent this, our system relies on the **ACID** properties of transactions:
- **Atomicity:** The check for overlapping bookings and the insertion of the new booking are treated as a single "all-or-nothing" unit. If any part fails, the whole operation is rolled back.
- **Consistency:** The database ensures that after the transaction, the equipment is not double-booked, maintaining the business rule that a resource cannot be in two places at once.
- **Isolation:** This is the key property for this milestone. We use a high isolation level to ensure that Thread B's "check" cannot see a state that is about to be invalidated by Thread A's "insert".
- **Durability:** Once the booking is confirmed, it is written to permanent storage and will survive system failures.

## 3. Chosen Isolation Level: SERIALIZABLE
We chose the `SERIALIZABLE` isolation level for the `bookEquipment` operation.
- **Why:** While lower levels like `READ_COMMITTED` or `REPEATABLE_READ` prevent "dirty reads" or "non-repeatable reads," they do not inherently prevent "phantom reads" or the specific race condition described above where a *new* record (the booking) is being added.
- **Mechanism:** In `SERIALIZABLE` mode, the database behaves as if transactions are executed sequentially. If User A and User B conflict, the database will detect the serialization failure. One transaction will succeed, and the other will be killed (rolled back) by the database with a "Serialization Failure" error.

## 4. Design Explanation & Implementation
### Transaction Management
We use Spring's `@Transactional(isolation = Isolation.SERIALIZABLE)` to wrap the business logic.

### Handling Serialization Failures (Retry Logic)
Because `SERIALIZABLE` transactions can be aborted by the database when conflicts occur, we implemented a **Retry Mechanism** using Spring Retry:
- **@Retryable:** The method is annotated to automatically retry up to 3 times if a `SQLException` or `TransientDataAccessException` (common for serialization failures) occurs.
- **Backoff:** We use an exponential backoff strategy to wait before retrying, reducing further contention.

### Code Snippets
**Service Layer:**
```java
@Retryable(
    retryFor = { SQLException.class, org.springframework.dao.TransientDataAccessException.class },
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
@Transactional(isolation = Isolation.SERIALIZABLE)
public Booking bookEquipment(Booking booking) {
    boolean isOverlapping = bookingRepository.existsOverlapping(...);
    if (isOverlapping) throw new RuntimeException("Already booked");
    return bookingRepository.save(booking);
}
```

**Repository Layer (SQL Check):**
```sql
SELECT COUNT(*) FROM booking 
WHERE equipment_id = ? AND status != 'CANCELLED' 
AND start_time < ? AND end_time > ?
```
