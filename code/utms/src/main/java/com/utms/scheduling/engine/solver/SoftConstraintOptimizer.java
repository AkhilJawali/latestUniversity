package com.utms.scheduling.engine.solver;

import com.utms.scheduling.engine.enums.SoftConstraintType;
import com.utms.scheduling.engine.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Computes soft constraint satisfaction (FR-5.1 through FR-5.6).
 * KD-49: Quality score = weighted average of 6 constraint satisfaction ratios.
 * PD-70: Weights from config table (default equal).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SoftConstraintOptimizer {

    public double computeQualityScore(CSPState state, SchedulingInput input) {
        Map<SoftConstraintType, Double> weights = input.getSoftConstraintWeights();
        double totalWeighted = 0.0, totalWeight = 0.0;

        double w1 = weights.getOrDefault(SoftConstraintType.FACULTY_TIME_PREFERENCE, 1.0);
        totalWeighted += w1 * computePreferenceSatisfaction(state, input);
        totalWeight += w1;

        double w2 = weights.getOrDefault(SoftConstraintType.FACULTY_DISTRIBUTION_PREFERENCE, 1.0);
        totalWeighted += w2 * computeDistributionSatisfaction(state, input);
        totalWeight += w2;

        double w3 = weights.getOrDefault(SoftConstraintType.ROOM_PROXIMITY, 1.0);
        totalWeighted += w3 * computeProximitySatisfaction(state, input);
        totalWeight += w3;

        double w4 = weights.getOrDefault(SoftConstraintType.GAP_MINIMIZATION, 1.0);
        totalWeighted += w4 * computeGapMinimization(state, input);
        totalWeight += w4;

        double w5 = weights.getOrDefault(SoftConstraintType.SOFT_BLOCK_OVERRIDE, 1.0);
        totalWeighted += w5 * computeSoftBlockAvoidance(state, input);
        totalWeight += w5;

        double w6 = weights.getOrDefault(SoftConstraintType.DAY_PATTERN_BALANCE, 1.0);
        totalWeighted += w6 * computeDayPatternBalance(state, input);
        totalWeight += w6;

        return totalWeight > 0 ? totalWeighted / totalWeight : 0.0;
    }

    public List<SoftViolation> identifyViolations(CSPState state, SchedulingInput input) {
        List<SoftViolation> violations = new ArrayList<>();

        // Soft block overrides
        for (int i = 0; i < state.getVariableCount(); i++) {
            if (!state.isAssigned(i)) continue;
            DaySlotRoom assignment = state.getAssignment(i);
            Long roomId = state.getActualRoomId(assignment.roomIndex());
            for (ActiveBlock block : input.getActiveBlocks()) {
                if ("SOFT".equals(block.getBlockType()) && block.getResourceId().equals(roomId)
                    && block.coversSlot(assignment.dayIndex(), assignment.slotIndex())) {
                    violations.add(new SoftViolation(SoftConstraintType.SOFT_BLOCK_OVERRIDE, "ROOM", roomId,
                        "Session placed in soft-blocked slot for room " + roomId,
                        "No alternative without hard constraint violation"));
                }
            }
        }
        // TODO: Add preference, gap, proximity, balance violations with full detail
        return violations;
    }

    private double computePreferenceSatisfaction(CSPState state, SchedulingInput input) {
        // TODO: Check faculty time-of-day preferences
        return 1.0;
    }

    private double computeDistributionSatisfaction(CSPState state, SchedulingInput input) {
        // TODO: Check consecutive vs spread preferences
        return 1.0;
    }

    private double computeProximitySatisfaction(CSPState state, SchedulingInput input) {
        int totalPairs = 0, sameBuildingPairs = 0;
        for (int i = 0; i < state.getVariableCount(); i++) {
            if (!state.isAssigned(i)) continue;
            for (int j = i + 1; j < state.getVariableCount(); j++) {
                if (!state.isAssigned(j)) continue;
                SessionVariable vi = state.getVariable(i), vj = state.getVariable(j);
                if (!vi.getFacultyId().equals(vj.getFacultyId()) && !vi.getBatchId().equals(vj.getBatchId())) continue;
                DaySlotRoom ai = state.getAssignment(i), aj = state.getAssignment(j);
                if (ai.dayIndex() != aj.dayIndex() || Math.abs(ai.slotIndex() - aj.slotIndex()) != 1) continue;
                totalPairs++;
                RoomInfo ri = input.getRooms().get(ai.roomIndex()), rj = input.getRooms().get(aj.roomIndex());
                if (ri.getBuilding() != null && ri.getBuilding().equals(rj.getBuilding())) sameBuildingPairs++;
            }
        }
        return totalPairs == 0 ? 1.0 : (double) sameBuildingPairs / totalPairs;
    }

    private double computeGapMinimization(CSPState state, SchedulingInput input) {
        // TODO: Count idle gaps per faculty/batch daily schedule
        return 1.0;
    }

    private double computeSoftBlockAvoidance(CSPState state, SchedulingInput input) {
        if (input.getActiveBlocks().isEmpty()) return 1.0;
        int total = state.getAssignedCount(), overrides = 0;
        for (int i = 0; i < state.getVariableCount(); i++) {
            if (!state.isAssigned(i)) continue;
            DaySlotRoom a = state.getAssignment(i);
            Long roomId = state.getActualRoomId(a.roomIndex());
            for (ActiveBlock block : input.getActiveBlocks()) {
                if ("SOFT".equals(block.getBlockType()) && block.getResourceId().equals(roomId)
                    && block.getDayOfWeek().equals(state.getDayName(a.dayIndex()))
                    && block.coversSlot(a.dayIndex(), a.slotIndex())) { overrides++; break; }
            }
        }
        return total == 0 ? 1.0 : 1.0 - ((double) overrides / total);
    }

    private double computeDayPatternBalance(CSPState state, SchedulingInput input) {
        int numDays = input.getWorkingDays().size();
        if (numDays == 0) return 1.0;
        int[] perDay = new int[numDays];
        for (int i = 0; i < state.getVariableCount(); i++) {
            if (!state.isAssigned(i)) continue;
            perDay[state.getAssignment(i).dayIndex()]++;
        }
        double mean = (double) state.getAssignedCount() / numDays;
        if (mean == 0) return 1.0;
        double variance = 0;
        for (int c : perDay) variance += Math.pow(c - mean, 2);
        variance /= numDays;
        double cv = Math.sqrt(variance) / mean;
        return Math.max(0.0, 1.0 - cv);
    }
}
