package com.utms.scheduling.engine.solver;

import lombok.Getter;

/**
 * Per-request context carrying timeout configuration and cancellation flag (KD-56).
 * Passed into the solver to control termination behavior.
 * Thread-safe: cancelRequested is volatile for cross-thread visibility.
 */
@Getter
public class SolverContext {

    private final long startTimeMs;
    private final long timeoutMs;
    private final Long requestId;
    private volatile boolean cancelRequested;

    public SolverContext(long startTimeMs, long timeoutMs, Long requestId) {
        this.startTimeMs = startTimeMs;
        this.timeoutMs = timeoutMs;
        this.requestId = requestId;
        this.cancelRequested = false;
    }

    /**
     * Check if the configured timeout has been exceeded.
     */
    public boolean isTimedOut() {
        return System.currentTimeMillis() - startTimeMs > timeoutMs;
    }

    /**
     * Signal cancellation. Called from the cancel endpoint thread.
     */
    public void requestCancel() {
        this.cancelRequested = true;
    }

    /**
     * Elapsed time since generation started, in milliseconds.
     */
    public long elapsedMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    /**
     * Elapsed time in seconds (for status reporting).
     */
    public long elapsedSeconds() {
        return elapsedMs() / 1000;
    }
}
