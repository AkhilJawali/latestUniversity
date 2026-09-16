package com.utms.scheduling.engine.entity;

import com.utms.common.entity.BaseEntity;
import com.utms.scheduling.engine.enums.DraftStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "timetable_drafts")
@Getter
@Setter
public class TimetableDraft extends BaseEntity {

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "semester", nullable = false, length = 20)
    private String semester;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DraftStatus status = DraftStatus.DRAFT;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "feasibility_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal feasibilityScore = BigDecimal.ZERO;

    @Column(name = "quality_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal qualityScore = BigDecimal.ZERO;

    @Column(name = "total_sessions_required", nullable = false)
    private Integer totalSessionsRequired = 0;

    @Column(name = "total_sessions_placed", nullable = false)
    private Integer totalSessionsPlaced = 0;

    @Column(name = "generation_request_id", nullable = false)
    private Long generationRequestId;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "is_partial", nullable = false)
    private Boolean isPartial = false;

    @Column(name = "partial_acknowledged", nullable = false)
    private Boolean partialAcknowledged = false;
}
