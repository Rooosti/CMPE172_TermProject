package com.example.termproject.service;

import com.example.termproject.model.Booking;
import com.example.termproject.model.Equipment;
import com.example.termproject.repository.BookingRepository;
import com.example.termproject.repository.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private com.example.termproject.repository.UserRepository userRepository;

    @Mock
    private CalendarIntegrationService calendarService;

    @InjectMocks
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void bookEquipmentCalculatesPriceCorrectly() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2026, 3, 10, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 10, 12, 30); // 2.5 hours -> 3 hours charged
        
        Equipment equipment = Equipment.builder()
                .equipmentId(1L)
                .hourlyRate(new BigDecimal("25.00"))
                .build();
        
        Booking booking = Booking.builder()
                .equipmentId(1L)
                .startTime(start)
                .endTime(end)
                .build();

        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));
        when(bookingRepository.existsOverlapping(any(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking result = bookingService.bookEquipment(booking);

        // Assert
        // 3 hours * 25.00 = 75.00
        assertEquals(new BigDecimal("75.00"), result.getTotalPrice());
    }

    @Test
    void bookEquipmentCalculatesPriceCorrectlyExactHours() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2026, 3, 10, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 10, 12, 0); // 2 hours exact
        
        Equipment equipment = Equipment.builder()
                .equipmentId(1L)
                .hourlyRate(new BigDecimal("25.00"))
                .build();
        
        Booking booking = Booking.builder()
                .equipmentId(1L)
                .startTime(start)
                .endTime(end)
                .build();

        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));
        when(bookingRepository.existsOverlapping(any(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking result = bookingService.bookEquipment(booking);

        // Assert
        // 2 hours * 25.00 = 50.00
        assertEquals(new BigDecimal("50.00"), result.getTotalPrice());
    }
}
