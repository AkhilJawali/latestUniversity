package com.utms.scheduling.engine.solver;

/**
 * Callback interface for reporting solver progress to the persistence layer (KD-57).
 * Called by the solver when the best-so-far checkpoint improves.
 * Implementations write to the database (throttled per OQ-D4).
 */
@FunctionalInterface
public interface ProgressCallback {

    /**
     * Called when the best-so-far count improves.
     *
     * @param newBestCount the new highest number of simultaneously placed sessions
     */
    void onCheckpointImproved(int newBestCount);
}
