package com.utms.scheduling.engine.solver;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Result of the constraint solver's solve phase.
 * Contains the CSP state (with assignments), the terminal outcome, and optional
 * infeasibility entries if the outcome is INFEASIBLE.
 *
 * If outcome is COMPLETE: state contains a full solution.
 * If outcome is TIMED_OUT/CANCELLED: state contains the best partial solution (Fix #3).
 * If outcome is INFEASIBLE: state contains partial + infeasibilityEntries explain why.
 */
@Getter
public class SolverResult {

    private final CSPState state;
    private final SolverOutcome outcome;
    private final List<InfeasibilityEntry> infeasibilityEntries;

    public SolverResult(CSPState state, SolverOutcome outcome) {
        this.state = state;
        this.outcome = outcome;
        this.infeasibilityEntries = Collections.emptyList();
    }

    public SolverResult(CSPState state, SolverOutcome outcome, List<InfeasibilityEntry> infeasibilityEntries) {
        this.state = state;
        this.outcome = outcome;
        this.infeasibilityEntries = infeasibilityEntries != null ? infeasibilityEntries : Collections.emptyList();
    }

    /**
     * Convenience: true if outcome is COMPLETE.
     * Maintained for backward compatibility with existing code that checks isSolved().
     */
    public boolean isSolved() {
        return outcome == SolverOutcome.COMPLETE;
    }
}
