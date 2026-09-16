package com.utms.masterdata.faculty.availability;

import java.time.LocalTime;
import java.util.List;

/**
 * Read contract for faculty availability, consumed by scheduling engine,
 * conflict detection, and substitution modules.
 */
public interface AvailabilityQueryService {

    /**
     * Returns the hard-blocked time ranges for a faculty member on a given day.
     * For BLOCKED mode: returns the declared windows directly.
     * For AVAILABLE mode: returns the declared available windows.
     */
    List<TimeRange> getHardBlockedSlots(Long facultyId, String dayOfWeek);

    /**
     * Returns the soft preferences for a faculty member.
     */
    FacultyPreferences getSoftPreferences(Long facultyId);

    /**
     * Checks if a faculty member is available during the specified time range on a given day.
     * Logic is inverted based on mode (BLOCKED vs AVAILABLE).
     */
    boolean isAvailable(Long facultyId, String dayOfWeek, LocalTime startTime, LocalTime endTime);

    /**
     * Returns the availability mode for a faculty member based on their designation.
     */
    AvailabilityMode getMode(Long facultyId);
}
