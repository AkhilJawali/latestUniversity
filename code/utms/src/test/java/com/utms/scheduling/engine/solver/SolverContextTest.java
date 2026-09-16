package com.utms.scheduling.engine.solver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SolverContext — verifies timeout detection, cancel signaling, and elapsed time.
 */
class SolverContextTest {

    @Test
    @DisplayName("isTimedOut returns false before timeout is reached")
    void isTimedOut_beforeTimeout_returnsFalse() {
        long startTime = System.currentTimeMillis();
        SolverContext context = new SolverContext(startTime, 60_000L, 1L);

        assertThat(context.isTimedOut()).isFalse();
    }

    @Test
    @DisplayName("isTimedOut returns true after timeout is exceeded")
    void isTimedOut_afterTimeout_returnsTrue() {
        // Set start time 70 seconds in the past with a 60-second timeout
        long startTime = System.currentTimeMillis() - 70_000;
        SolverContext context = new SolverContext(startTime, 60_000L, 1L);

        assertThat(context.isTimedOut()).isTrue();
    }

    @Test
    @DisplayName("isTimedOut returns false at exactly the boundary (within margin)")
    void isTimedOut_atBoundary_returnsFalse() {
        // Start time is just barely within timeout
        long startTime = System.currentTimeMillis() - 59_000;
        SolverContext context = new SolverContext(startTime, 60_000L, 1L);

        assertThat(context.isTimedOut()).isFalse();
    }

    @Test
    @DisplayName("requestCancel sets cancelRequested flag")
    void requestCancel_setsCancelFlag() {
        SolverContext context = new SolverContext(System.currentTimeMillis(), 120_000L, 1L);

        assertThat(context.isCancelRequested()).isFalse();

        context.requestCancel();

        assertThat(context.isCancelRequested()).isTrue();
    }

    @Test
    @DisplayName("requestCancel is idempotent")
    void requestCancel_calledMultipleTimes_remainsTrue() {
        SolverContext context = new SolverContext(System.currentTimeMillis(), 120_000L, 1L);

        context.requestCancel();
        context.requestCancel();

        assertThat(context.isCancelRequested()).isTrue();
    }

    @Test
    @DisplayName("elapsedMs returns approximately correct elapsed time")
    void elapsedMs_returnsApproximateElapsed() {
        long startTime = System.currentTimeMillis() - 5000;
        SolverContext context = new SolverContext(startTime, 120_000L, 1L);

        long elapsed = context.elapsedMs();

        // Allow 100ms margin for test execution time
        assertThat(elapsed).isBetween(4900L, 5200L);
    }

    @Test
    @DisplayName("elapsedSeconds returns seconds from elapsedMs")
    void elapsedSeconds_returnsSecondsConversion() {
        long startTime = System.currentTimeMillis() - 10_000;
        SolverContext context = new SolverContext(startTime, 120_000L, 1L);

        long elapsedSec = context.elapsedSeconds();

        assertThat(elapsedSec).isBetween(9L, 11L);
    }

    @Test
    @DisplayName("getTimeoutMs returns configured timeout")
    void getTimeoutMs_returnsConfiguredValue() {
        SolverContext context = new SolverContext(System.currentTimeMillis(), 90_000L, 42L);

        assertThat(context.getTimeoutMs()).isEqualTo(90_000L);
    }

    @Test
    @DisplayName("getRequestId returns the request identifier")
    void getRequestId_returnsConfiguredValue() {
        SolverContext context = new SolverContext(System.currentTimeMillis(), 120_000L, 99L);

        assertThat(context.getRequestId()).isEqualTo(99L);
    }
}
