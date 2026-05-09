package com.example.termproject.repository;

import com.example.termproject.model.Booking;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    Booking save(Booking booking);
    Optional<Booking> findById(Long id);
    List<Booking> findByRenterId(Long renterId);
    List<Booking> findByEquipmentId(Long equipmentId);
    List<Booking> findByProviderId(Long providerId);

    // Core for concurrency control: Check if equipment is busy during this time
    boolean existsOverlapping(Long equipmentId, LocalDateTime start, LocalDateTime end);

    // Find users who have either rented from this user or this user has rented from
    List<Long> findPartnersByUserId(Long userId);
}
