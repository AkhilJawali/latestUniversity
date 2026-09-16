package com.utms.scheduling.engine.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

/**
 * A time slot available for scheduling (from TimeSlotGridService).
 */
@Getter
@Builder
public class SlotInfo {
    private final Long slotDefinitionId;
    private final int index;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int durationMinutes;
    private final String slotType; // TEACHING, BREAK, LUNCH
    private final String dayOfWeek;
}
