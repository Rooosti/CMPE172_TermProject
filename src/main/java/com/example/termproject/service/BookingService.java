package com.example.termproject.service;

import com.example.termproject.model.Booking;
import com.example.termproject.model.Equipment;
import com.example.termproject.model.User;
import com.example.termproject.repository.BookingRepository;
import com.example.termproject.repository.EquipmentRepository;
import com.example.termproject.repository.UserRepository;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final CalendarIntegrationService calendarService;

    public BookingService(BookingRepository bookingRepository,
            EquipmentRepository equipmentRepository,
            UserRepository userRepository,
            CalendarIntegrationService calendarService) {
        this.bookingRepository = bookingRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.calendarService = calendarService;
    }

    /**
     * Creates a new booking with strict concurrency control.
     * Use SERIALIZABLE isolation to prevent race conditions where two threads
     * check availability simultaneously and both succeed before either inserts.
     * 
     * @Retryable handles the case where the DB detects a serialization failure
     *            and kills one of the transactions (common in SERIALIZABLE).
     */
    @Retryable(retryFor = { SQLException.class,
            org.springframework.dao.TransientDataAccessException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Timed(value = "booking.operation.duration", description = "Time taken to process a rental")
    public Booking bookEquipment(Booking booking) {
        logger.info(">>> [BOOKING_ATTEMPT] User {} attempting to rent Equipment {}: {} - {}", 
                booking.getRenterId(), booking.getEquipmentId(), booking.getStartTime(), booking.getEndTime());
        
        try {
            // 1. Concurrency Check: Double-booking prevention
            boolean isOverlapping = bookingRepository.existsOverlapping(
                    booking.getEquipmentId(),
                    booking.getStartTime(),
                    booking.getEndTime());

            if (isOverlapping) {
                logger.warn(">>> [BOOKING_REJECTED] Conflict detected for Equipment {} at {}", 
                        booking.getEquipmentId(), booking.getStartTime());
                throw new RuntimeException("Equipment is already booked for the selected time slot.");
            }

            // 2. Business Logic: Calculate Price
            Equipment equipment = equipmentRepository.findById(booking.getEquipmentId())
                    .orElseThrow(() -> new RuntimeException("Equipment not found"));

            Duration duration = Duration.between(booking.getStartTime(), booking.getEndTime());
            long hours = duration.toHours();
            if (duration.toMinutesPart() > 0) {
                hours++; // Round up to nearest hour for partial hours
            }

            BigDecimal totalPrice = equipment.getHourlyRate().multiply(BigDecimal.valueOf(hours));
            booking.setTotalPrice(totalPrice);

            // 3. Business Logic: Default status
            if (booking.getStatus() == null) {
                booking.setStatus("CONFIRMED");
            }

            // 4. Save to Database
            Booking savedBooking = bookingRepository.save(booking);

            // 5. Milestone 5: Distribution Boundary
            // Conceptually send the appointment info to the external Google Calendar service
            User renter = userRepository.findById(booking.getRenterId()).orElse(null);
            if (renter != null) {
                calendarService.syncToGoogleCalendar(savedBooking, renter, equipment);
            }

            logger.info(">>> [BOOKING_SUCCESS] Booking ID: {}. Status: {}. Total Price: ${}", 
                    savedBooking.getBookingId(), savedBooking.getStatus(), savedBooking.getTotalPrice());

            return savedBooking;
        } catch (Exception e) {
            logger.error(">>> [BOOKING_FAILURE] Reason: {}. Data: {}", e.getMessage(), booking);
            throw e;
        }
    }

    public List<Booking> getRenterHistory(Long renterId) {
        return bookingRepository.findByRenterId(renterId);
    }

    public List<Booking> getEquipmentSchedule(Long equipmentId) {
        return bookingRepository.findByEquipmentId(equipmentId);
    }

    public List<Booking> getIncomingAppointments(Long providerId) {
        return bookingRepository.findByProviderId(providerId);
    }
}
