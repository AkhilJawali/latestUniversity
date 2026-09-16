package com.utms.scheduling.engine.enums;

/**
 * Why a session is treated as fixed (immovable) during (partial) re-generation (A4-14).
 * LOCKED: coordinator locked it (HC-LOCK-1). APPROVED: an approved section (HC-LOCK-2).
 * OUT_OF_SCOPE: not selected by the regeneration scope (HC-LOCK-3). Also used for
 * institution common slots pre-placed as fixed occupancy (KD-59).
 */
public enum FixedReason {
    LOCKED,
    APPROVED,
    OUT_OF_SCOPE
}
