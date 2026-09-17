package com.utms.scheduling.conflict;

import com.utms.conflict.detection.PlacementRuleChecker;
import com.utms.scheduling.engine.enums.RecurrenceType;
import com.utms.scheduling.engine.enums.WeekGroup;
import com.utms.scheduling.engine.model.FacultyWorkloadLimits;
import com.utms.scheduling.engine.service.RecurrenceOverlapEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PlacementRuleChecker} — A4-16 acceptance criteria AC1–AC7.
 * The recurrence evaluator is the real component; repositories and the faculty-limit
 * provider are stubbed.
 */
class PlacementRuleCheckerTest {

    private FacultyLimitProvider facultyLimitProvider;
    private PlacementRuleChecker checker;

    private static final String DAY = "MONDAY";
    private static final Long SLOT = 3L;

    @BeforeEach
    void setUp() {
        facultyLimitProvider = mock(FacultyLimitProvider.class);
        // Default: no faculty limits (data-pending); capacity lookups empty (pass).
        when(facultyLimitProvider.getLimits(anyLong())).thenReturn(Optional.empty());
        checker = new PlacementRuleChecker(facultyLimitProvider, new RecurrenceOverlapEvaluator());
    }

    private ProposedPlacementRequest placement(Long faculty, Long room, Long batch) {
        return ProposedPlacementRequest.builder()
                .facultyId(faculty).roomId(room).batchId(batch)
                .dayOfWeek(DAY).slotDefinitionId(SLOT).durationMinutes(60)
                .build();
    }

    private DraftOccupancyIndex indexWith(DraftOccupancyIndex.Occupant... occupants) {
        DraftOccupancyIndex index = new DraftOccupancyIndex();
        for (DraftOccupancyIndex.Occupant o : occupants) {
            index.add(o);
        }
        return index;
    }

    private DraftOccupancyIndex.Occupant weekly(Long sessionId, Long faculty, Long room, Long batch,
                                                String day, Long slot) {
        return new DraftOccupancyIndex.Occupant(sessionId, faculty, room, batch, null, day, slot,
                1.0, RecurrenceType.WEEKLY, null);
    }

    // AC1
    @Test
    void check_facultyAlreadyBookedInSlot_returnsFacultyDoubleBooking() {
        DraftOccupancyIndex index = indexWith(weekly(10L, 1L, 99L, 88L, DAY, SLOT));
        List<ConflictDto> conflicts = checker.check(placement(1L, 5L, 7L), index);
        assertThat(conflicts).extracting(ConflictDto::getType).contains(ConflictType.FACULTY_DOUBLE_BOOKING);
    }

    // AC2
    @Test
    void check_roomAlreadyBookedInSlot_returnsRoomDoubleBooking() {
        DraftOccupancyIndex index = indexWith(weekly(10L, 99L, 5L, 88L, DAY, SLOT));
        List<ConflictDto> conflicts = checker.check(placement(1L, 5L, 7L), index);
        assertThat(conflicts).extracting(ConflictDto::getType).contains(ConflictType.ROOM_DOUBLE_BOOKING);
    }

    // AC3
    @Test
    void check_batchAlreadyBookedInSlot_returnsBatchClash() {
        DraftOccupancyIndex index = indexWith(weekly(10L, 99L, 88L, 7L, DAY, SLOT));
        List<ConflictDto> conflicts = checker.check(placement(1L, 5L, 7L), index);
        assertThat(conflicts).extracting(ConflictDto::getType).contains(ConflictType.BATCH_CLASH);
    }

    // AC4
    @Test
    void check_roomCapacityLessThanBatchStrength_returnsCapacityConflict() {
        Room room = new Room();
        room.setCapacity(40);
        Batch batch = new Batch();
        batch.setStrength(60);
        when(roomRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(room));
        when(batchRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(batch));

        List<ConflictDto> conflicts = checker.check(placement(1L, 5L, 7L), new DraftOccupancyIndex());
        assertThat(conflicts).extracting(ConflictDto::getType).contains(ConflictType.ROOM_CAPACITY);
    }

    // AC6 (no conflict happy path)
    @Test
    void check_freeSlotNoRuleViolation_returnsEmpty() {
        when(roomRepository.findByIdAndDeletedAtIsNull(anyLong())).thenReturn(Optional.empty());
        when(batchRepository.findByIdAndDeletedAtIsNull(anyLong())).thenReturn(Optional.empty());
        List<ConflictDto> conflicts = checker.check(placement(1L, 5L, 7L), new DraftOccupancyIndex());
        assertThat(conflicts).isEmpty();
    }

    // AC7 (multiple conflicts at once — same faculty AND same room occupant)
    @Test
    void check_placementViolatesTwoRules_returnsAllConflicts() {
        DraftOccupancyIndex index = indexWith(weekly(10L, 1L, 5L, 88L, DAY, SLOT));
        List<ConflictDto> conflicts = checker.check(placement(1L, 5L, 7L), index);
        assertThat(conflicts).extracting(ConflictDto::getType)
                .contains(ConflictType.FACULTY_DOUBLE_BOOKING, ConflictType.ROOM_DOUBLE_BOOKING);
    }

    // KD-64 recurrence gate: a WEEKLY proposal co-occurs with a FORTNIGHTLY occupant,
    // so the same-slot faculty clash IS reported (the gate lets it through).
    @Test
    void check_weeklyProposalVsFortnightlyOccupant_reportsClash() {
        DraftOccupancyIndex index = new DraftOccupancyIndex();
        index.add(new DraftOccupancyIndex.Occupant(10L, 1L, 5L, 7L, null, DAY, SLOT,
                1.0, RecurrenceType.FORTNIGHTLY, WeekGroup.WEEK_B));
        List<ConflictDto> conflicts = checker.check(placement(1L, 5L, 7L), index);
        assertThat(conflicts).extracting(ConflictDto::getType).contains(ConflictType.FACULTY_DOUBLE_BOOKING);
    }

    // KD-64 direct: opposite fortnightly groups never co-occur (the gate would suppress).
    @Test
    void recurrenceGate_oppositeFortnightlyGroups_neverCoOccur() {
        RecurrenceOverlapEvaluator eval = new RecurrenceOverlapEvaluator();
        boolean coOccur = eval.everCoOccur(
                RecurrenceOverlapEvaluator.SessionRecurrence.fortnightly(WeekGroup.WEEK_A),
                RecurrenceOverlapEvaluator.SessionRecurrence.fortnightly(WeekGroup.WEEK_B));
        assertThat(coOccur).isFalse();
    }

    // AC5 (consecutive hours) — with limits supplied via the provider
    @Test
    void check_consecutiveHoursExceedLimit_returnsConsecutiveConflict() {
        // Faculty 1 has a 60-min session at 09:00-10:00 (slot 2); proposal is 10:00-11:00
        // (slot 3). Consecutive run = 2h; limit = 1h -> violation.
        when(facultyLimitProvider.getLimits(1L)).thenReturn(Optional.of(FacultyWorkloadLimits.builder()
                .facultyId(1L).maxDailyHours(8).maxWeeklyHours(40).maxConsecutiveHours(1).build()));

        SlotDefinition slot2 = new SlotDefinition();
        slot2.setStartTime(LocalTime.of(9, 0));
        slot2.setEndTime(LocalTime.of(10, 0));
        SlotDefinition slot3 = new SlotDefinition();
        slot3.setStartTime(LocalTime.of(10, 0));
        slot3.setEndTime(LocalTime.of(11, 0));
        when(slotDefinitionRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(slot2));
        when(slotDefinitionRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(slot3));

        DraftOccupancyIndex index = new DraftOccupancyIndex();
        index.add(new DraftOccupancyIndex.Occupant(10L, 1L, 6L, 8L, null, DAY, 2L,
                1.0, RecurrenceType.WEEKLY, null));

        ProposedPlacementRequest p = ProposedPlacementRequest.builder()
                .facultyId(1L).roomId(5L).batchId(7L).dayOfWeek(DAY).slotDefinitionId(3L)
                .durationMinutes(60).build();

        List<ConflictDto> conflicts = checker.check(p, index);
        assertThat(conflicts).extracting(ConflictDto::getType).contains(ConflictType.FACULTY_CONSECUTIVE_HOURS);
    }

    // AC5 companion: no limits supplied -> workload rules do not fire (data-pending)
    @Test
    void check_consecutiveHoursButNoLimitsLoaded_noWorkloadConflict() {
        SlotDefinition slot2 = new SlotDefinition();
        slot2.setStartTime(LocalTime.of(9, 0));
        slot2.setEndTime(LocalTime.of(10, 0));
        when(slotDefinitionRepository.findByIdAndDeletedAtIsNull(anyLong())).thenReturn(Optional.of(slot2));
        DraftOccupancyIndex index = new DraftOccupancyIndex();
        index.add(new DraftOccupancyIndex.Occupant(10L, 1L, 6L, 8L, null, DAY, 2L,
                1.0, RecurrenceType.WEEKLY, null));

        List<ConflictDto> conflicts = checker.check(placement(1L, 5L, 7L), index);
        assertThat(conflicts).extracting(ConflictDto::getType)
                .doesNotContain(ConflictType.FACULTY_CONSECUTIVE_HOURS,
                        ConflictType.FACULTY_DAILY_HOURS, ConflictType.FACULTY_WEEKLY_HOURS);
    }
}
