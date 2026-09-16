package com.utms.scheduling.engine.entity;

import com.utms.common.entity.BaseEntity;
import com.utms.scheduling.engine.enums.GenerationPhase;
import com.utms.scheduling.engine.enums.GenerationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "generation_requests")
@Getter
@Setter
public class GenerationRequest extends BaseEntity {

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "semester", nullable = false, length = 20)
    private String semester;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GenerationStatus status = GenerationStatus.IN_PROGRESS;

    @Column(name = "progress", nullable = false)
    private Integer progress = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", length = 30)
    private GenerationPhase phase;

    @Column(name = "seed")
    private Long seed;

    @Column(name = "triggered_by", nullable = false, length = 100)
    private String triggeredBy;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "draft_id")
    private Long draftId;

    @Column(name = "elapsed_ms")
    private Long elapsedMs;

    @Column(name = "best_so_far_count", nullable = false)
    private Integer bestSoFarCount = 0;

    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(name = "timeout_duration_seconds", nullable = false)
    private Integer timeoutDurationSeconds = 120;

    // A4-14 (KD-61): partial re-generation provenance. NULL for a full generation.
    // The source draft this partial re-generation was based on.
    @Column(name = "source_draft_id")
    private Long sourceDraftId;

    // JSON selector {batchIds, sectionIds, courseIds} for a partial re-generation.
    @Column(name = "regeneration_scope", columnDefinition = "TEXT")
    private String regenerationScope;
}
