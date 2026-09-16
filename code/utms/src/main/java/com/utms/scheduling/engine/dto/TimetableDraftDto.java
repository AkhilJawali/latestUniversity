package com.utms.scheduling.engine.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TimetableDraftDto {
    private final Long id;
    private final Long departmentId;
    private final String semester;
    private final String academicYear;
    private final String status;
    private final Integer version;
    private final BigDecimal feasibilityScore;
    private final BigDecimal qualityScore;
    private final Integer totalSessionsRequired;
    private final Integer totalSessionsPlaced;
    private final Long generationRequestId;
    private final LocalDateTime generatedAt;
    private final Integer violationCount;
}
