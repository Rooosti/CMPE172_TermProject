package com.example.termproject.service;

import com.example.termproject.model.Booking;
import com.example.termproject.model.Equipment;
import com.example.termproject.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Milestone 5: Distribution Boundary Design (Conceptual)
 * 
 * This service represents the conceptual REST call to Google Calendar API.
 * It demonstrates a coarse-grained API call across a process boundary.
 */
@Service
public class CalendarIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(CalendarIntegrationService.class);

    // Theoretical Google Calendar API Endpoint
    private static final String GOOGLE_CALENDAR_API_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";

    /**
     * Conceptual coarse-grained REST call to create a Google Calendar event.
     * In a real implementation, this would use RestTemplate/WebClient with an
     * OAuth2 token.
     */
    public void syncToGoogleCalendar(Booking booking, User renter, Equipment equipment) {
        // Constructing the coarse-grained payload (as specified in Milestone 5)
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("summary", "Equipment Rental: " + equipment.getName());
        eventPayload.put("description", "Renter: " + renter.getFirstName() + " " + renter.getLastName() +
                "\nBooking ID: " + booking.getBookingId());

        Map<String, String> start = new HashMap<>();
        start.put("dateTime", booking.getStartTime().format(DateTimeFormatter.ISO_DATE_TIME));
        start.put("timeZone", "UTC");

        Map<String, String> end = new HashMap<>();
        end.put("dateTime", booking.getEndTime().format(DateTimeFormatter.ISO_DATE_TIME));
        end.put("timeZone", "UTC");

        eventPayload.put("start", start);
        eventPayload.put("end", end);

        // --- Milestone 5: The "Distribution Boundary" ---
        // Log the conceptual REST call to the outside world
        logger.info(">>> [DISTRIBUTION BOUNDARY] Conceptual REST POST to: " + GOOGLE_CALENDAR_API_URL);
        logger.info(">>> Payload: " + eventPayload.toString());
    }
}
