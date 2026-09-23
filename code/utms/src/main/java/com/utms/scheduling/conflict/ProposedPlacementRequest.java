package com.utms.scheduling.conflict;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for checking a proposed session placement.
 * 
 * Used by the frontend drag-drop interaction to validate a placement
 * before committing it to the draft.
 * 
 * Design: A4-16 §6 API Design
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposedPlacementRequest {

    /** Draft context for the placement */
    @NotNull
    private Long draftId;

    /** Session being moved/placed (null for new session) */
    private Long sessionId;

    /** Proposed room */
    @NotNull
    private Long roomId;

    /** Proposed faculty */
    @NotNull
    private Long facultyId;

    /** Proposed batch */
    @NotNull
    private Long batchId;

    /** Proposed section (optional) */
    private Long sectionId;

    /** Proposed day (MONDAY, TUESDAY, etc.) */
    @NotNull
    private String dayOfWeek;

    /** Proposed slot definition ID */
    @NotNull
    private Long slotDefinitionId;

    /** Duration in minutes (default: 60) */
    private Integer durationMinutes;
}
