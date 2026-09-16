package com.utms.scheduling.engine.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Workload limits for a faculty member (KD-53).
 * Data source: A4-32 cadre norms with per-faculty override from A4-4 (PD-19).
 * Load measured in HOURS (sum of slot durations), not session count.
 */
@Getter
@Builder
public class FacultyWorkloadLimits {
    private final Long facultyId;
    private final double maxDailyHours;
    private final double maxWeeklyHours;
    private final double maxConsecutiveHours;
}
