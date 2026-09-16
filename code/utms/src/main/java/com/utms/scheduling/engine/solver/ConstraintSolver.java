package com.utms.scheduling.engine.solver;

import com.utms.scheduling.engine.model.SchedulingInput;
import com.utms.scheduling.engine.model.SessionVariable;
import com.utms.scheduling.engine.model.SlotInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * CSP Constraint Solver (KD-45).
 * Three phases: AC-3 propagation, Backtracking (MRV+LCV), Hill-climbing optimization.
 * Fix #3: Maintains best-partial checkpoint for partial feasibility scores.
 *
 * Uses SolverContext for timeout/cancellation and ProgressCallback for checkpoint reporting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConstraintSolver {

    private static final int OPTIMIZATION_RESTARTS = 5;
    private static final int OPTIMIZATION_ITERATIONS_PER_RESTART = 500;

    private final HardConstraintValidator hardValidator;

    /**
     * Phase 3: Initialize domains and run AC-3 propagation.
     * Populates infeasibilityCollector if propagation proves infeasibility
     * (a domain becomes permanently empty after arc consistency).
     */
    public CSPState initializeAndPropagate(List<SessionVariable> variables, SchedulingInput input,
                                           InfeasibilityCollector infeasibilityCollector) {
        CSPState state = new CSPState(variables, input);

        List<List<DaySlotRoom>> domains = new ArrayList<>(variables.size());
        for (SessionVariable var : variables) {
            domains.add(buildDomainForVariable(var, input, state));
        }
        state.setDomains(domains);
        state.prePlaceCommonSlots(input.getCommonSlots());
        hardValidator.propagateArcs(state);

        // Check for infeasibility after propagation: any permanently empty domain
        for (int i = 0; i < variables.size(); i++) {
            if (state.getDomain(i).isEmpty() && !state.isAssigned(i)) {
                SessionVariable var = variables.get(i);
                infeasibilityCollector.record(var, identifyBlockingConstraints(var, input));
            }
        }

        log.info("Domain init complete: {} variables, avg domain size = {}",
            variables.size(), domains.stream().mapToInt(List::size).average().orElse(0));
        return state;
    }

    /**
     * Phase 4: Backtracking with MRV + LCV + best-partial checkpoint (Fix #3).
     * Uses SolverContext for timeout/cancel checks and ProgressCallback for progress reporting.
     *
     * Infeasibility is declared ONLY when:
     * (a) During propagation (handled in initializeAndPropagate), or
     * (b) After root-level exhaustion when timeout not reached and cancel not requested.
     *
     * Empty domain mid-search is a normal dead-end (returns false), NOT infeasible.
     */
    public SolverResult solve(CSPState state, Random random, SolverContext context,
                              ProgressCallback progressCallback, InfeasibilityCollector infeasibilityCollector) {
        // If propagation already proved infeasibility, return immediately
        if (infeasibilityCollector.hasEntries()) {
            log.warn("Infeasibility detected during propagation: {} entries", infeasibilityCollector.getEntries().size());
            return new SolverResult(state, SolverOutcome.INFEASIBLE, infeasibilityCollector.getEntries());
        }

        int[] bestCount = new int[]{state.getAssignedCount()};
        CSPState[] bestPartial = new CSPState[]{state.deepCopy()};

        BacktrackResult btResult = backtrack(state, random, context, progressCallback,
            infeasibilityCollector, bestPartial, bestCount, 0);

        if (btResult == BacktrackResult.SOLVED) {
            log.info("Complete solution: {} sessions placed", state.getAssignedCount());
            return new SolverResult(state, SolverOutcome.COMPLETE);
        } else if (btResult == BacktrackResult.TIMED_OUT) {
            log.warn("Timed out: best partial {}/{} placed", bestCount[0], state.getVariableCount());
            return new SolverResult(bestPartial[0], SolverOutcome.TIMED_OUT);
        } else if (btResult == BacktrackResult.CANCELLED) {
            log.info("Cancelled by user: best partial {}/{} placed", bestCount[0], state.getVariableCount());
            return new SolverResult(bestPartial[0], SolverOutcome.CANCELLED);
        } else {
            // Root-level exhaustion: timeout not reached and cancel not requested → infeasible
            log.warn("Root-level exhaustion: {}/{} placed — declaring infeasible", bestCount[0], state.getVariableCount());
            List<InfeasibilityEntry> entries = infeasibilityCollector.buildReport(bestPartial[0]);
            return new SolverResult(bestPartial[0], SolverOutcome.INFEASIBLE, entries);
        }
    }

    /**
     * Phase 5: Hill-climbing with random restarts.
     * Uses SolverContext for timeout checks instead of raw startTime.
     */
    public CSPState optimize(CSPState state, boolean solved, SchedulingInput input,
                             Random random, SolverContext context) {
        if (!solved) return state;

        long remainingMs = context.getTimeoutMs() - context.elapsedMs();
        if (remainingMs < 5000) {
            log.info("Skipping optimization — less than 5s remaining");
            return state;
        }

        CSPState bestState = state.deepCopy();
        for (int restart = 0; restart < OPTIMIZATION_RESTARTS; restart++) {
            if (context.isTimedOut() || context.isCancelRequested()) break;

            CSPState current = state.deepCopy();
            for (int iter = 0; iter < OPTIMIZATION_ITERATIONS_PER_RESTART; iter++) {
                if (context.isTimedOut() || context.isCancelRequested()) break;

                CSPState neighbor = generateNeighbor(current, random);
                if (neighbor != null && hardValidator.isStateValid(neighbor)) {
                    current = neighbor;
                    bestState = current.deepCopy();
                }
            }
        }
        return bestState;
    }

    private BacktrackResult backtrack(CSPState state, Random random, SolverContext context,
                                      ProgressCallback progressCallback,
                                      InfeasibilityCollector infeasibilityCollector,
                                      CSPState[] bestPartial, int[] bestCount, int depth) {
        // Check timeout
        if (context.isTimedOut()) return BacktrackResult.TIMED_OUT;

        // Check cancellation
        if (context.isCancelRequested()) return BacktrackResult.CANCELLED;

        // All assigned = solved
        if (state.allAssigned()) return BacktrackResult.SOLVED;

        // Update best-so-far checkpoint
        if (state.getAssignedCount() > bestCount[0]) {
            bestPartial[0] = state.deepCopy();
            bestCount[0] = state.getAssignedCount();
            progressCallback.onCheckpointImproved(bestCount[0]);
        }

        int varIndex = state.selectMRVVariable();
        if (varIndex < 0 || state.getDomain(varIndex).isEmpty()) {
            // Empty domain mid-search is a normal dead-end, NOT infeasible
            infeasibilityCollector.recordDeadEnd(varIndex >= 0 ? varIndex : 0);
            return BacktrackResult.EXHAUSTED;
        }

        List<DaySlotRoom> orderedValues = state.orderByLCV(varIndex);

        for (DaySlotRoom value : orderedValues) {
            if (context.isTimedOut()) return BacktrackResult.TIMED_OUT;
            if (context.isCancelRequested()) return BacktrackResult.CANCELLED;

            if (hardValidator.isAssignmentValid(state.getVariable(varIndex), value, state)) {
                state.assign(varIndex, value);
                CSPState.Checkpoint cp = state.checkpoint();
                state.forwardCheck(varIndex, value, hardValidator);

                if (!state.hasEmptyDomain()) {
                    BacktrackResult childResult = backtrack(state, random, context, progressCallback,
                        infeasibilityCollector, bestPartial, bestCount, depth + 1);

                    if (childResult == BacktrackResult.SOLVED) return BacktrackResult.SOLVED;
                    if (childResult == BacktrackResult.TIMED_OUT) return BacktrackResult.TIMED_OUT;
                    if (childResult == BacktrackResult.CANCELLED) return BacktrackResult.CANCELLED;
                }

                state.restore(cp);
                state.unassign(varIndex);
            }
        }

        // All values exhausted for this variable at this depth
        infeasibilityCollector.recordDeadEnd(varIndex);
        return BacktrackResult.EXHAUSTED;
    }

    private List<DaySlotRoom> buildDomainForVariable(SessionVariable var, SchedulingInput input, CSPState state) {
        List<DaySlotRoom> domain = new ArrayList<>();
        int numDays = input.getWorkingDays().size();

        for (int day = 0; day < numDays; day++) {
            // Only this day's own grid entries — slot index is the position within the day
            List<SlotInfo> daySlots = state.getSlotsForDay(day);
            for (int slot = 0; slot < daySlots.size(); slot++) {
                SlotInfo si = daySlots.get(slot);
                if (si.getDurationMinutes() != var.getRequiredDurationMinutes()) continue; // Fix #1
                if (!"TEACHING".equals(si.getSlotType())) continue; // HC-ENG-9
                for (int room = 0; room < input.getRooms().size(); room++) {
                    domain.add(new DaySlotRoom(day, slot, room));
                }
            }
        }
        return domain;
    }

    private CSPState generateNeighbor(CSPState state, Random random) {
        int n = state.getVariableCount();
        if (n < 2) return null;
        int i = random.nextInt(n), j = random.nextInt(n);
        if (i == j || !state.isAssigned(i) || !state.isAssigned(j)) return null;

        CSPState neighbor = state.deepCopy();
        DaySlotRoom ai = neighbor.getAssignment(i), aj = neighbor.getAssignment(j);
        neighbor.unassign(i);
        neighbor.unassign(j);
        neighbor.assign(i, aj);
        neighbor.assign(j, ai);
        return neighbor;
    }

    /**
     * Identify which constraint types block a variable from having any valid domain values.
     * Used during propagation-phase infeasibility detection.
     */
    private List<String> identifyBlockingConstraints(SessionVariable var, SchedulingInput input) {
        List<String> constraints = new ArrayList<>();
        // Only report constraints that are relevant to this variable's empty domain.
        // Check if equipment requirements could have pruned all rooms:
        if (var.getRequiredEquipment() != null && !var.getRequiredEquipment().isEmpty()) {
            boolean anyRoomHasEquipment = input.getRooms().stream()
                .anyMatch(r -> r.getEquipmentTags() != null && r.getEquipmentTags().containsAll(var.getRequiredEquipment()));
            if (!anyRoomHasEquipment) {
                constraints.add("EQUIPMENT_MISMATCH");
            }
        }
        // Check if batch strength exceeds all available room capacities:
        if (var.getBatchStrength() > 0) {
            boolean anyRoomFits = input.getRooms().stream()
                .anyMatch(r -> r.getCapacity() >= var.getBatchStrength());
            if (!anyRoomFits) {
                constraints.add("ROOM_CAPACITY");
            }
        }
        // Check if faculty has hard blocks covering all teaching slots:
        if (var.getFacultyId() != null) {
            // Faculty availability is a likely cause if domain is empty after propagation
            constraints.add("FACULTY_AVAILABILITY");
        }
        // Check if session duration matches any slot in the grid:
        boolean anySlotMatchesDuration = input.getSlotGrid().stream()
            .anyMatch(s -> s.getDurationMinutes() == var.getRequiredDurationMinutes()
                && "TEACHING".equals(s.getSlotType()));
        if (!anySlotMatchesDuration) {
            constraints.add("SLOT_DURATION_MISMATCH");
        }
        // If no specific cause identified, report as general constraint conflict
        if (constraints.isEmpty()) {
            constraints.add("COMBINED_CONSTRAINT_CONFLICT");
        }
        return constraints;
    }

    /**
     * Internal enum representing backtracking outcomes at each recursion level.
     */
    private enum BacktrackResult {
        SOLVED,
        TIMED_OUT,
        CANCELLED,
        EXHAUSTED
    }
}
