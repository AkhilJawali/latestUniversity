package com.utms.approval.entity;

import com.utms.approval.enums.WorkflowAction;
import com.utms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A single immutable entry in the approval audit trail (A4-19 FR-4 / HC-AW-3). One row per
 * submit/approve/reject action, capturing who ({@code actorUserId}), when ({@code actedAt}),
 * the {@link WorkflowAction}, the level (index + name), optional comments, and — for a
 * rejection — the mandatory {@code rejectionReason}. Append-only: the service never updates
 * or deletes these rows.
 */
@Entity
@Table(name = "workflow_steps")
@Getter
@Setter
public class WorkflowStep extends BaseEntity {

    @Column(name = "workflow_instance_id", nullable = false)
    private Long workflowInstanceId;

    @Column(name = "level_index", nullable = false)
    private Integer levelIndex;

    @Column(name = "level_name", nullable = false, length = 100)
    private String levelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private WorkflowAction action;

    @Column(name = "actor_user_id", nullable = false, length = 100)
    private String actorUserId;

    @Column(name = "comments", length = 2000)
    private String comments;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;
}
