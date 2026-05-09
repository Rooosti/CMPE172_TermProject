package com.example.termproject.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {
    private Long equipmentId;
    private Long providerId;
    private String name;
    private String description;
    private BigDecimal hourlyRate;
    private boolean isDeactivated;
    private boolean isReported;
}
