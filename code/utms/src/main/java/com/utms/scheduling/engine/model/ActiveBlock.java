package com.utms.scheduling.engine.model;

import lombok.Builder;
import lombok.Getter;

/**
 * An active resource block loaded for scheduling (from BlockService).
 */
@Getter
@Builder
public class ActiveBlock {
    private final Long blockId;
    private final String blockType; // HARD, SOFT
    private final String resourceType; // ROOM, ASSET
    private final Long resourceId;
    private final String dayOfWeek;
    private final int startSlotIndex;
    private final int endSlotIndex;

    public boolean coversSlot(int dayIndex, int slotIndex) {
        return slotIndex >= startSlotIndex && slotIndex <= endSlotIndex;
    }
}
