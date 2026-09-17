package com.utms.scheduling.conflict;

import com.utms.scheduling.engine.model.FacultyWorkloadLimits;

import java.util.Optional;

/**
 * Seam for supplying per-faculty workload limits to the real-time conflict checker
 * (A4-16). Kept as an interface so the workload rules (daily/weekly/consecutive) are
 * fully implemented and unit-testable now, while the concrete master-data source is
 * wired later.
 *
 * <p>Current state (honest): there is no loaded faculty-limits master data in the
 * system — the engine's {@code SchedulingDataLoader.loadFacultyLimits} is a stub
 * returning an empty list (blocked on A4-4 / A4-32). The default bean
 * {@link EmptyFacultyLimitProvider} therefore returns {@link Optional#empty()}, and
 * the workload rules degrade to "no violation" (matching the engine's own
 * {@code CSPState} behavior). When the faculty-limits data source lands, a real
 * provider bean replaces the default and the workload rules begin firing with no
 * change to the checker.
 */
public interface FacultyLimitProvider {

    /**
     * Get workload limits for a faculty member.
     *
     * @param facultyId the faculty ID
     * @return the limits if configured/available, otherwise empty
     */
    Optional<FacultyWorkloadLimits> getLimits(Long facultyId);
}
