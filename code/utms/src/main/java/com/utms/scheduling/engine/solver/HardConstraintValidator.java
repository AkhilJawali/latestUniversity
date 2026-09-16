package com.utms.scheduling.engine.solver;

import com.utms.scheduling.engine.model.SessionVariable;
import org.springframework.stereotype.Component;

/**
 * Validates all 12 hard constraints (HC-ENG-1 through HC-ENG-12).
 * Uses CSPState's BitSet occupancy maps for O(1) conflict detection (KD-48).
 * HC-ENG-8/9/10 are structural (handled at domain build time / pre-placement).
 */
@Component
public class HardConstraintValidator {

    /**
     * Check if assigning the given variable to the given slot violates any hard constraint.
     * Returns true if the assignment is VALID (no violations).
     */
    public boolean isAssignmentValid(SessionVariable var, DaySlotRoom assignment, CSPState state) {
        int day = assignment.dayIndex();
        int slot = assignment.slotIndex();
        int room = assignment.roomIndex();

        // HC-ENG-1: Faculty non-double-booking
        if (state.getFacultyOccupancy(var.getFacultyId(), day, slot)) return false;

        // HC-ENG-2: Room non-double-booking
        if (state.getRoomOccupancy(room, day, slot)) return false;

        // HC-ENG-3: Batch non-clash
        if (state.getBatchOccupancy(var.getBatchId(), day, slot)) return false;

        // HC-ENG-4: Room capacity >= batch strength
        if (state.getRoomCapacity(room) < var.getBatchStrength()) return false;

        // HC-ENG-5: Equipment tag matching
        if (!state.roomHasEquipment(room, var.getRequiredEquipment())) return false;

        // HC-ENG-6: Faculty hard availability block
        if (state.isFacultyHardBlocked(var.getFacultyId(), day, slot)) return false;

        // HC-ENG-7: Hard resource block on room
        if (state.isRoomHardBlocked(room, day, slot)) return false;

        // HC-ENG-8: Calendar exclusion — structural (non-working days excluded from domain)
        // HC-ENG-9: Grid conformance — structural (only matching-duration teaching slots in domain)
        // HC-ENG-10: Common slot reservation — structural (pre-placed in KD-52)

        // HC-ENG-11: Faculty max daily/weekly load (KD-53: hours)
        if (state.wouldExceedDailyLoad(var.getFacultyId(), day)) return false;
        if (state.wouldExceedWeeklyLoad(var.getFacultyId())) return false;

        // HC-ENG-12: Faculty max consecutive hours
        if (state.wouldExceedConsecutive(var.getFacultyId(), day, slot)) return false;

        return true;
    }

    /**
     * Validate the entire state (all assignments) — used after optimization swaps.
     */
    public boolean isStateValid(CSPState state) {
        for (int i = 0; i < state.getVariableCount(); i++) {
            if (!state.isAssigned(i)) continue;
            DaySlotRoom assignment = state.getAssignment(i);
            state.unassign(i);
            boolean valid = isAssignmentValid(state.getVariable(i), assignment, state);
            state.assign(i, assignment);
            if (!valid) return false;
        }
        return true;
    }

    /**
     * AC-3 arc propagation — prunes domains based on constraints.
     * Iterates until no more pruning occurs (fixed-point).
     */
    public void propagateArcs(CSPState state) {
        boolean changed = true;
        int iterations = 0;
        int maxIterations = 100;

        while (changed && iterations < maxIterations) {
            changed = false;
            iterations++;
            for (int i = 0; i < state.getVariableCount(); i++) {
                if (state.isAssigned(i)) continue;
                final int varIndex = i;
                int before = state.getDomain(varIndex).size();
                state.getDomain(varIndex).removeIf(candidate ->
                    !isAssignmentValid(state.getVariable(varIndex), candidate, state));
                if (state.getDomain(varIndex).size() < before) {
                    changed = true;
                }
            }
        }
    }
}
