package com.utms.masterdata.academiccalendar;

import java.time.LocalDate;

/**
 * Domain event emitted when a calendar change (holiday add, pattern change) may impact
 * already-scheduled sessions. Consumed by conflict detection (FR-7.1, KD-38).
 * The system flags impacted sessions but does NOT auto-cancel them (FR-7.3).
 */
public record CalendarImpactEvent(
        Long campusId,
        LocalDate affectedStartDate,
        LocalDate affectedEndDate,
        String changeType,
        String description
) {}
