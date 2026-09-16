package com.utms.approval.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** A4-19 PD-101 — a configurable pipeline with its ordered levels (read-only). */
@Getter
@Builder
public class ApprovalPipelineDto {
    private final Long id;
    private final String name;
    private final String scope;
    private final Boolean isActive;
    private final List<ApprovalLevelDto> levels;
}
