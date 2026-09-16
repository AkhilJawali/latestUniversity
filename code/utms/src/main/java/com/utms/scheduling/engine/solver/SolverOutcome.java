package com.utms.scheduling.engine.solver;

/**
 * Terminal outcome of a solver run (KD-55).
 * Distinguishes between complete success, timeout, proven infeasibility, and cancellation.
 */
public enum SolverOutcome {
    /** All sessions placed successfully. */
    COMPLETE,
    /** Time limit reached; best partial solution returned. */
    TIMED_OUT,
    /** Engine proved no valid complete solution exists. */
    INFEASIBLE,
    /** Coordinator cancelled the run. */
    CANCELLED
}
