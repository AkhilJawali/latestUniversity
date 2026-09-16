package com.utms.scheduling.engine.solver;

import com.utms.scheduling.engine.enums.SessionType;
import com.utms.scheduling.engine.model.CommonSlotInfo;
import com.utms.scheduling.engine.model.FacultyWorkloadLimits;
import com.utms.scheduling.engine.model.RoomInfo;
import com.utms.scheduling.engine.model.SchedulingInput;
import com.utms.scheduling.engine.model.SessionVariable;
import com.utms.scheduling.engine.model.SlotInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConstraintSolver slot addressing — every physical (day, slot definition) must be
 * exactly one solver cell, so the same period cannot be double-booked through another day's grid entries.
 */
class ConstraintSolverTest {

    private static final long FACULTY_ID = 200L;

    private final ConstraintSolver solver = new ConstraintSolver(new HardConstraintValidator());

    @Test
    @DisplayName("sessions sharing faculty, batch and room land in distinct physical (day, slot) cells")
    void solve_sameFacultyBatchRoom_placesInDistinctDaySlots() {
        SchedulingInput input = input(List.of(slot(1L, "MONDAY", 9), slot(1L, "TUESDAY", 9)), List.of(), List.of());

        SolverResult result = run(lectures(2), input);

        assertThat(result.getOutcome()).isEqualTo(SolverOutcome.COMPLETE);
        assertThat(placedCells(result.getState())).containsExactlyInAnyOrder("MONDAY|1", "TUESDAY|1");
    }

    @Test
    @DisplayName("more sessions than physical cells is INFEASIBLE, never a double-booked COMPLETE")
    void solve_moreSessionsThanDaySlots_isInfeasible() {
        SchedulingInput input = input(List.of(slot(1L, "MONDAY", 9), slot(1L, "TUESDAY", 9)), List.of(), List.of());

        SolverResult result = run(lectures(3), input);

        assertThat(result.getOutcome()).isEqualTo(SolverOutcome.INFEASIBLE);
        assertThat(placedCells(result.getState())).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("slot id is resolved from the assigned day's own grid entry")
    void getActualSlotId_daySpecificSlots_resolvesPerDay() {
        SchedulingInput input = input(List.of(slot(1L, "MONDAY", 9), slot(2L, "TUESDAY", 9)), List.of(), List.of());

        SolverResult result = run(lectures(2), input);

        assertThat(result.getOutcome()).isEqualTo(SolverOutcome.COMPLETE);
        assertThat(placedCells(result.getState())).containsExactlyInAnyOrder("MONDAY|1", "TUESDAY|2");
    }

    @Test
    @DisplayName("common slot blocks its own day's slot, not the first day carrying the same slot id")
    void solve_commonSlotOnTuesday_blocksTuesdayOnly() {
        CommonSlotInfo tuesdayCommon = CommonSlotInfo.builder()
            .id(1L).name("CCC").dayOfWeek("TUESDAY").slotDefinitionId(1L).appliesToAllBatches(true).build();
        SchedulingInput input = input(List.of(slot(1L, "MONDAY", 9), slot(1L, "TUESDAY", 9)),
            List.of(tuesdayCommon), List.of());

        SolverResult oneSession = run(lectures(1), input);
        SolverResult twoSessions = run(lectures(2), input);

        assertThat(oneSession.getOutcome()).isEqualTo(SolverOutcome.COMPLETE);
        assertThat(placedCells(oneSession.getState())).containsExactly("MONDAY|1");
        assertThat(twoSessions.getOutcome()).isEqualTo(SolverOutcome.INFEASIBLE);
    }

    @Test
    @DisplayName("consecutive-hours limit counts adjacent slots within a day, not across day boundaries")
    void solve_maxConsecutiveOneHour_allowsOneSessionPerDayOnly() {
        List<SlotInfo> grid = List.of(
            slot(1L, "MONDAY", 9), slot(2L, "MONDAY", 10),
            slot(1L, "TUESDAY", 9), slot(2L, "TUESDAY", 10));
        FacultyWorkloadLimits limits = FacultyWorkloadLimits.builder()
            .facultyId(FACULTY_ID).maxDailyHours(8.0).maxWeeklyHours(40.0).maxConsecutiveHours(1.0).build();
        SchedulingInput input = input(grid, List.of(), List.of(limits));

        SolverResult twoSessions = run(lectures(2), input);
        SolverResult threeSessions = run(lectures(3), input);

        assertThat(twoSessions.getOutcome()).isEqualTo(SolverOutcome.COMPLETE);
        assertThat(placedCells(twoSessions.getState()))
            .extracting(cell -> cell.substring(0, cell.indexOf('|')))
            .containsExactlyInAnyOrder("MONDAY", "TUESDAY");
        assertThat(threeSessions.getOutcome()).isEqualTo(SolverOutcome.INFEASIBLE);
    }

    private SolverResult run(List<SessionVariable> vars, SchedulingInput input) {
        InfeasibilityCollector collector = new InfeasibilityCollector(vars);
        CSPState state = solver.initializeAndPropagate(vars, input, collector);
        SolverContext context = new SolverContext(System.currentTimeMillis(), 10_000L, 1L);
        return solver.solve(state, new Random(42), context, count -> { }, collector);
    }

    private static List<String> placedCells(CSPState state) {
        List<String> cells = new ArrayList<>();
        for (int i = 0; i < state.getVariableCount(); i++) {
            if (!state.isAssigned(i)) continue;
            DaySlotRoom a = state.getAssignment(i);
            cells.add(state.getDayName(a.dayIndex()) + "|" + state.getActualSlotId(a.dayIndex(), a.slotIndex()));
        }
        return cells;
    }

    private static SchedulingInput input(List<SlotInfo> slotGrid, List<CommonSlotInfo> commonSlots,
                                         List<FacultyWorkloadLimits> facultyLimits) {
        return SchedulingInput.builder()
            .departmentId(1L).campusId(1L).semester("ODD")
            .courses(List.of())
            .facultyLimits(facultyLimits)
            .rooms(List.of(RoomInfo.builder().roomId(10L).roomName("LH-1").capacity(60).equipmentTags(List.of()).build()))
            .slotGrid(slotGrid)
            .workingDays(List.of("MONDAY", "TUESDAY"))
            .activeBlocks(List.of())
            .commonSlots(commonSlots)
            .derivationRules(List.of())
            .softConstraintWeights(Map.of())
            .build();
    }

    private static SlotInfo slot(Long slotDefinitionId, String day, int startHour) {
        return SlotInfo.builder()
            .slotDefinitionId(slotDefinitionId).index(startHour - 9)
            .startTime(LocalTime.of(startHour, 0)).endTime(LocalTime.of(startHour + 1, 0))
            .durationMinutes(60).slotType("TEACHING").dayOfWeek(day)
            .build();
    }

    private static List<SessionVariable> lectures(int count) {
        return IntStream.range(0, count).mapToObj(i -> SessionVariable.builder()
            .courseId(100L).facultyId(FACULTY_ID).batchId(300L)
            .sessionType(SessionType.LECTURE).requiredDurationMinutes(60)
            .requiredEquipment(List.of()).batchStrength(30).sequenceIndex(i)
            .build()).toList();
    }
}
