package com.utms.scheduling.engine.model;

import com.utms.scheduling.engine.enums.SessionType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * A CSP variable representing one session to be placed (KD-47).
 * Each session must be assigned to exactly one (day, slot, room) tuple
 * WHERE slot.durationMinutes == requiredDurationMinutes (Fix #1).
 */
@Getter
@Builder
public class SessionVariable {
    private final Long courseId;
    private final Long facultyId;
    private final Long batchId;
    private final Long sectionId;
    private final SessionType sessionType;
    private final int requiredDurationMinutes; // Fix #1: duration matching
    private final List<String> requiredEquipment;
    private final int batchStrength;
    private final int sequenceIndex; // Fix #9: stable sort key (not indexOf)
}
