package com.utms.scheduling.conflict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * A proposed session placement to be checked for conflicts (A4-16, design FR-1).
 *
 * <p>Server-side validated (allowlist) before processing. {@code sessionId} is
 * optional: when present it identifies the session being moved so it can be
 * self-excluded from the draft's occupancy (a move onto its own slot is not a
 * self-conflict).
 *
 * <p>Design spec: {facultyId!, roomId!, batchId!, sectionId?, dayOfWeek!,
 * slotDefinitionId!, durationMinutes!, sessionId?} (! = required/positive)
 */
@Getter
@Setter
@Builder
public class ProposedPlacementRequest {

    @NotNull(message = "facultyId is required")
    @Positive(message = "facultyId must be positive")
    private Long facultyId;

    @NotNull(message = "roomId is required")
    @Positive(message = "roomId must be positive")
    private Long roomId;

    @NotNull(message = "batchId is required")
    @Positive(message = "batchId must be positive")
    private Long batchId;

    /** Optional: sub-group / section, when the placement is section-scoped. */
    private Long sectionId;

    @NotBlank(message = "dayOfWeek is required")
    private String dayOfWeek;

    @NotNull(message = "slotDefinitionId is required")
    @Positive(message = "slotDefinitionId must be positive")
    private Long slotDefinitionId;

    @NotNull(message = "durationMinutes is required")
    @Positive(message = "durationMinutes must be positive")
    private Integer durationMinutes;

    /** Optional: the session being moved, excluded from occupancy during the check. */
    private Long sessionId;
}
