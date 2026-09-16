package com.utms.scheduling.engine.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO representing a single conflict within an infeasibility report.
 */
@Getter
@Builder
public class InfeasibilityConflictDto {
    private final Long id;
    private final String affectedSessionDescription;
    private final String conflictingConstraints;
    private final String explanation;
}
