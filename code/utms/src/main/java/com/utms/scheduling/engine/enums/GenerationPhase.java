package com.utms.scheduling.engine.enums;

/**
 * Current phase of the generation pipeline (for progress reporting).
 */
public enum GenerationPhase {
    LOADING_DATA,
    DERIVING_SESSIONS,
    PROPAGATING,
    SOLVING,
    OPTIMIZING,
    STORING_RESULTS
}
