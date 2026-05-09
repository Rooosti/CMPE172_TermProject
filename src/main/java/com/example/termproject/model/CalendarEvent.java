package com.example.termproject.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEvent {
    private String summary;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;
    private String attendeeEmail;
}
