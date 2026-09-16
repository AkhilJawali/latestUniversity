package com.utms.scheduling.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class DerivationRuleDto {
    private Long id;
    private Long campusId;
    private String componentType;
    private Integer slotDurationMinutes;
    private BigDecimal hoursPerSession;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
