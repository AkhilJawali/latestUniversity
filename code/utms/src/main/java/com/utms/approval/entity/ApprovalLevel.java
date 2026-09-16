package com.utms.approval.entity;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * An ordered level within an {@link ApprovalPipeline} (A4-19 FR-6). {@code levelIndex} is
 * 0-based; level 0 is the drafting/Coordinator level and review begins at level 1
 * (KD-A19-2). {@code requiredRole} is advisory until RBAC lands (PD-103).
 */
@Entity
@Table(name = "approval_levels")
@Getter
@Setter
public class ApprovalLevel extends BaseEntity {

    @Column(name = "pipeline_id", nullable = false)
    private Long pipelineId;

    @Column(name = "level_index", nullable = false)
    private Integer levelIndex;

    @Column(name = "level_name", nullable = false, length = 100)
    private String levelName;

    @Column(name = "required_role", length = 50)
    private String requiredRole;
}
