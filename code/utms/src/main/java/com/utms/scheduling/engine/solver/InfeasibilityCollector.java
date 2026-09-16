package com.utms.scheduling.engine.solver;

import com.utms.scheduling.engine.model.SessionVariable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects infeasibility evidence during solver execution.
 * NOT a Spring bean — instantiated per solver run to avoid shared state across requests.
 *
 * Records two types of evidence:
 * 1. Propagation-phase: domains permanently emptied (proven infeasible before search).
 * 2. Search-phase: dead-end frequency per variable index for root-level exhaustion reporting.
 */
public class InfeasibilityCollector {

    private final List<InfeasibilityEntry> entries = new ArrayList<>();
    private final Map<Integer, Integer> deadEndFrequency = new HashMap<>();
    private final List<SessionVariable> variables;

    /**
     * Create a collector bound to the session variables for this run.
     *
     * @param variables the list of CSP variables (sessions to place)
     */
    public InfeasibilityCollector(List<SessionVariable> variables) {
        this.variables = variables;
    }

    /**
     * Record an infeasibility entry from propagation phase.
     * Called when a variable's domain is permanently empty after AC-3.
     *
     * @param variable the session variable that cannot be placed
     * @param constraintTypes list of constraint type names that caused the empty domain
     */
    public void record(SessionVariable variable, List<String> constraintTypes) {
        String description = buildSessionDescription(variable);
        String explanation = String.format(
            "Session '%s' has no valid (day, slot, room) assignment after constraint propagation. "
            + "Blocking constraints: %s",
            description,
            String.join(", ", constraintTypes)
        );
        entries.add(new InfeasibilityEntry(description, constraintTypes, explanation));
    }

    /**
     * Record a dead-end occurrence during backtracking search.
     * A dead-end means all values for a variable at some search depth were exhausted.
     * High frequency indicates the variable is heavily constrained.
     *
     * @param varIndex the variable index that hit a dead-end
     */
    public void recordDeadEnd(int varIndex) {
        deadEndFrequency.merge(varIndex, 1, Integer::sum);
    }

    /**
     * Build an infeasibility report from dead-end frequency data.
     * Called after root-level exhaustion (timeout not reached, cancel not requested)
     * to produce human-readable entries identifying the most constrained sessions.
     *
     * @param state the best partial CSPState at exhaustion time
     * @return list of InfeasibilityEntry describing unplaceable sessions
     */
    public List<InfeasibilityEntry> buildReport(CSPState state) {
        if (!entries.isEmpty()) {
            // Already have propagation-phase entries; return those
            return new ArrayList<>(entries);
        }

        List<InfeasibilityEntry> report = new ArrayList<>();

        // Find unassigned variables — those are the sessions that couldn't be placed
        for (int i = 0; i < state.getVariableCount(); i++) {
            if (!state.isAssigned(i)) {
                SessionVariable var = variables.get(i);
                String description = buildSessionDescription(var);
                int deadEnds = deadEndFrequency.getOrDefault(i, 0);

                List<String> constraintTypes = inferConstraintTypes(deadEnds);
                String explanation = String.format(
                    "Session '%s' could not be placed after exhaustive search. "
                    + "Dead-end frequency: %d (higher = more constrained). "
                    + "Likely blocking factors: %s",
                    description, deadEnds, String.join(", ", constraintTypes)
                );
                report.add(new InfeasibilityEntry(description, constraintTypes, explanation));
            }
        }

        // Sort by dead-end frequency (most constrained first)
        report.sort(Comparator.comparingInt(e -> {
            int idx = findVariableIndex(e.sessionDescription());
            return -deadEndFrequency.getOrDefault(idx, 0);
        }));

        return report;
    }

    /**
     * Check if any infeasibility entries have been recorded (propagation-phase).
     */
    public boolean hasEntries() {
        return !entries.isEmpty();
    }

    /**
     * Get all recorded infeasibility entries (propagation-phase only).
     */
    public List<InfeasibilityEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    /**
     * Get the dead-end frequency map (variable index to count).
     * Useful for diagnostics and testing.
     */
    public Map<Integer, Integer> getDeadEndFrequency() {
        return new HashMap<>(deadEndFrequency);
    }

    private String buildSessionDescription(SessionVariable var) {
        return String.format("Course=%d, Faculty=%d, Batch=%d, Type=%s, Duration=%dmin",
            var.getCourseId(), var.getFacultyId(), var.getBatchId(),
            var.getSessionType(), var.getRequiredDurationMinutes());
    }

    private List<String> inferConstraintTypes(int deadEndCount) {
        List<String> types = new ArrayList<>();
        types.add("RESOURCE_CONTENTION");
        if (deadEndCount > 50) {
            types.add("FACULTY_DOUBLE_BOOKING");
            types.add("BATCH_CLASH");
        }
        if (deadEndCount > 100) {
            types.add("ROOM_CAPACITY");
            types.add("EQUIPMENT_MISMATCH");
        }
        return types;
    }

    private int findVariableIndex(String description) {
        for (int i = 0; i < variables.size(); i++) {
            if (buildSessionDescription(variables.get(i)).equals(description)) {
                return i;
            }
        }
        return 0;
    }
}
