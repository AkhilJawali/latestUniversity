package com.utms.scheduling.engine.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for the infeasibility report returned by GET /generate/{requestId}/infeasibility.
 */
@Getter
@Builder
public class InfeasibilityReportDto {
    private final Long id;
    private final Long generationRequestId;
    private final LocalDateTime detectedAt;
    private final String summary;
    private final List<InfeasibilityConflictDto> conflicts;
}
