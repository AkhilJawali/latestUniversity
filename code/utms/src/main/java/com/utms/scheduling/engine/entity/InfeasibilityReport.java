package com.utms.scheduling.engine.entity;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores infeasibility analysis when the engine proves no valid solution exists (KD-58).
 * One report per GenerationRequest (1:1).
 */
@Entity
@Table(name = "infeasibility_reports")
@Getter
@Setter
public class InfeasibilityReport extends BaseEntity {

    @Column(name = "generation_request_id", nullable = false)
    private Long generationRequestId;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InfeasibilityConflict> conflicts = new ArrayList<>();
}
