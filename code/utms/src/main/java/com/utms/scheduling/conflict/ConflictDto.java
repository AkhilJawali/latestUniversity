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
 */
@Getter
@Builder
public class ConflictDto {
    private final ConflictType type;
    private final Long involvedFacultyId;
    private final Long involvedRoomId;
    private final Long involvedBatchId;
    private final Long involvedSectionId;
    private final Long involvedSessionId;
    private final String dayOfWeek;
    private final Long slotDefinitionId;
    private final String description;
}
