package com.utms.scheduling.engine.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Institution-level common slot (CCC/UWE) — pre-placed blocker (KD-52).
 */
@Getter
@Builder
public class CommonSlotInfo {
    private final Long id;
    private final String name;
    private final String dayOfWeek;
    private final Long slotDefinitionId;
    private final boolean appliesToAllBatches;
}
