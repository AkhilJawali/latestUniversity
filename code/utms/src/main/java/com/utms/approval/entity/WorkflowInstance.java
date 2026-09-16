package com.utms.approval.entity;

import com.utms.approval.enums.WorkflowState;
import com.utms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * One approval workflow instance per draft submission (A4-19 FR-1). Tracks the pipeline in
 * use, the current level index, and the overall {@link WorkflowState}. {@code @Version}
 * gives optimistic concurrency (PD-105): a stale approve/reject fails with 409.
 * At most one IN_REVIEW instance per draft (HC-AW-1, enforced by a partial unique index).
 */
@Entity
@Table(name = "workflow_instances")
@Getter
@Setter
public class WorkflowInstance extends BaseEntity {

    @Column(name = "draft_id", nullable = false)
    private Long draftId;

    @Column(name = "pipeline_id", nullable = false)
    private Long pipelineId;

    @Column(name = "current_level_index", nullable = false)
    private Integer currentLevelIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private WorkflowState state = WorkflowState.IN_REVIEW;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
