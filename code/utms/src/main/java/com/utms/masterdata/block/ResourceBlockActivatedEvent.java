package com.utms.masterdata.block;

/**
 * Published when a resource block transitions to ACTIVE status.
 * Consumed by A4-16 integration (conflict detection / session displacement).
 */
public record ResourceBlockActivatedEvent(
        Long blockId,
        String resourceType,
        Long resourceId
) {
}
