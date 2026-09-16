package com.utms.scheduling.engine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * Response listing the actual dates a session occurs within the semester
 * (A4-13, design section 3, FR-4.2 / FR-6.1). Holiday/non-working occurrences
 * are already excluded (PD-82). For a weekly session this is every working
 * occurrence of its day.
 */
@Getter
@Builder
@Schema(description = "The dates a session actually occurs within the semester (holidays excluded)")
public class OccurrenceDatesDto {
    @Schema(description = "Scheduled session id")
    private final Long sessionId;
    @Schema(description = "Recurrence type", example = "FORTNIGHTLY")
    private final String recurrenceType;
    @Schema(description = "Week group for fortnightly sessions; null when weekly", example = "WEEK_A")
    private final String weekGroup;
    @Schema(description = "Occurrence dates in ascending order")
    private final List<LocalDate> occurrenceDates;
}
