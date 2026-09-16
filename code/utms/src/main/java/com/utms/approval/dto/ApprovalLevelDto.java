package com.utms.approval.dto;

import lombok.Builder;
import lombok.Getter;

/** A4-19 PD-101 — a level within a pipeline (read-only). */
@Getter
@Builder
public class ApprovalLevelDto {
    private final Long id;
    private final Integer levelIndex;
    private final String levelName;
    private final String requiredRole;
}
