package com.utms.scheduling.conflict;

import com.utms.scheduling.engine.model.FacultyWorkloadLimits;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Default {@link FacultyLimitProvider} used until a real faculty-limits master-data
 * source is wired (A4-4 / A4-32). Returns no limits, so the workload rules
 * (daily/weekly/consecutive) do not fire — the honest "data-pending" state for A4-16.
 *
 * <p>When the faculty-limits data source lands, add a real provider bean annotated
 * {@code @Primary} so it takes precedence over this default with no change to
 * {@link PlacementRuleChecker}.
 */
@Component
public class EmptyFacultyLimitProvider implements FacultyLimitProvider {

    @Override
    public Optional<FacultyWorkloadLimits> getLimits(Long facultyId) {
        return Optional.empty();
    }
}
