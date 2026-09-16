package com.utms.scheduling.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SoftConstraintWeightDto {
    private Long id;
    private Long campusId;
    private String constraintType;
    private BigDecimal weight;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
