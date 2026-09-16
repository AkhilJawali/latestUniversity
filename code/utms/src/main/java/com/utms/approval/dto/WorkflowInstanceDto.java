package com.utms.approval.dto;

import com.utms.approval.enums.WorkflowState;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** A4-19 FR-5 — a workflow instance with its current position, the draft it governs, and
 *  the ordered approval history. */
@Getter
@Builder
public class WorkflowInstanceDto {
    private final Long id;
    private final Long draftId;
    private final String draftStatus;
    private final Long pipelineId;
    private final Integer currentLevelIndex;
    private final String currentLevelName;
    private final WorkflowState state;
    private final List<WorkflowStepDto> steps;
}
