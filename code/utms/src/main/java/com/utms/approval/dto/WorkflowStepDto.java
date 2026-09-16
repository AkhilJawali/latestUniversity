package com.utms.approval.dto;

import com.utms.approval.enums.WorkflowAction;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** A4-19 FR-5 — one audit-trail entry in a workflow instance's history. */
@Getter
@Builder
public class WorkflowStepDto {
    private final Long id;
    private final Integer levelIndex;
    private final String levelName;
    private final WorkflowAction action;
    private final String actorUserId;
    private final String comments;
    private final String rejectionReason;
    private final LocalDateTime actedAt;
}
