package com.utms.scheduling.engine.entity;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A single unplaceable session within an infeasibility report (KD-58).
 * Records which session could not be placed and which constraints caused the conflict.
 */
@Entity
@Table(name = "infeasibility_conflicts")
@Getter
@Setter
public class InfeasibilityConflict extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private InfeasibilityReport report;

    @Column(name = "affected_session_description", nullable = false, length = 500)
    private String affectedSessionDescription;

    @Column(name = "conflicting_constraints", nullable = false, columnDefinition = "TEXT")
    private String conflictingConstraints;

    @Column(name = "explanation", nullable = false, columnDefinition = "TEXT")
    private String explanation;
}
