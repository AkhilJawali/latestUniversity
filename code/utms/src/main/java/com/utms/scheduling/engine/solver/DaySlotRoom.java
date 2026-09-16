package com.utms.scheduling.engine.solver;

/**
 * A candidate assignment for a session variable: (day index, slot index, room index).
 * These are array indices, not database IDs: dayIndex into SchedulingInput.workingDays,
 * slotIndex into that day's slot list (CSPState.getSlotsForDay), roomIndex into SchedulingInput.rooms.
 */
public record DaySlotRoom(int dayIndex, int slotIndex, int roomIndex) {
}
