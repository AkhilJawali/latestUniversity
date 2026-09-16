package com.utms.scheduling.engine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * Response describing a session's recurrence pattern (A4-13, design section 3).
 * {@code weekGroup} is null for weekly sessions.
 */
@Getter
@Builder
@Schema(description = "A scheduled session's recurrence pattern")
public class SessionRecurrenceDto {
    @Schema(description = "Scheduled session id")
    private final Long sessionId;
    @Schema(description = "Day of week the session is placed on", example = "MONDAY")
    private final String dayOfWeek;
    @Schema(description = "Time-slot definition id")
    private final Long slotDefinitionId;
    @Schema(description = "Recurrence type", example = "FORTNIGHTLY")
    private final String recurrenceType;
    @Schema(description = "Week group for fortnightly sessions; null when weekly", example = "WEEK_A")
    private final String weekGroup;
}
