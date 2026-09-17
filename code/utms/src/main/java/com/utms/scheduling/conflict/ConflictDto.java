package com.utms.scheduling.conflict;

import lombok.Builder;
import lombok.Getter;

/**
 * A single real-time conflict returned by the conflict detection engine
 * (A4-16, design FR-4). Transient — not persisted (PD-97).
 *
 * <p>Carries a machine-readable {@link ConflictType}, the identifiers of the
 * entities involved, the (day, slot) at which the conflict occurs, and a
 * human-readable description that never exposes internal details.
 *
 * <p>Design spec: { type, involvedFacultyId?, involvedRoomId?, involvedBatchId?,
 * involvedSectionId?, involvedSessionId?, dayOfWeek, slotDefinitionId, description }
 */
@Getter
@Builder
public class ConflictDto {

    /** The type of conflict detected. */
    private final ConflictType type;

    /** The faculty involved in the conflict (if applicable). */
    private final Long involvedFacultyId;

    /** The room involved in the conflict (if applicable). */
    private final Long involvedRoomId;

    /** The batch involved in the conflict (if applicable). */
    private final Long involvedBatchId;

    /** The section involved in the conflict (if applicable). */
    private final Long involvedSectionId;

    /** The existing session that conflicts with the proposed placement. */
    private final Long involvedSessionId;

    /** The day of week where the conflict occurs (e.g., "MONDAY"). */
    private final String dayOfWeek;

    /** The slot definition ID where the conflict occurs. */
    private final Long slotDefinitionId;

    /** Human-readable description for the editor UI. Never exposes internals. */
    private final String description;
}
