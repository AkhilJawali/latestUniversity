package com.utms.scheduling.conflict;

import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a detected conflict for API responses and WebSocket broadcasts.
 * 
 * Design: A4-16 §2.2 ConflictDto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictDto {

    /** Conflict type (e.g., FACULTY_DOUBLE_BOOKING) */
    private ConflictType type;

    /** Human-readable label (e.g., "Faculty double-booking") */
    private String label;

    /** Internal (within draft) or CROSS_DRAFT */
    private String scope;

    /** Session that triggered the conflict */
    private Long sessionId;

    /** Day of week (MONDAY, TUESDAY, etc.) */
    private String dayOfWeek;

    /** Slot definition ID */
    private Long slotDefinitionId;

    /** Room ID involved in conflict */
    private Long roomId;

    /** Faculty ID involved in conflict */
    private Long facultyId;

    /** Batch ID involved in conflict */
    private Long batchId;

    /** Section ID involved in conflict */
    private Long sectionId;

    /** Session IDs of conflicting sessions (for resolution UI) */
    private List<Long> conflictingSessionIds;

    /** Human-readable description for UI display */
    private String description;

    /**
     * Creates a ConflictDto from a conflict result.
     */
    public static ConflictDto from(
            ConflictType type,
            Long sessionId,
            String dayOfWeek,
            Long slotDefinitionId,
            Long roomId,
            Long facultyId,
            Long batchId,
            Long sectionId,
            List<Long> conflictingSessionIds,
            String description) {
        return ConflictDto.builder()
                .type(type)
                .label(type.getLabel())
                .scope(type.getScope().getLabel())
                .sessionId(sessionId)
                .dayOfWeek(dayOfWeek)
                .slotDefinitionId(slotDefinitionId)
                .roomId(roomId)
                .facultyId(facultyId)
                .batchId(batchId)
                .sectionId(sectionId)
                .conflictingSessionIds(conflictingSessionIds)
                .description(description)
                .build();
    }
}
