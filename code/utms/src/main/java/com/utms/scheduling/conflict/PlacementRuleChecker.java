package com.utms.scheduling.conflict;

import com.utms.masterdata.batch.Batch;
import com.utms.masterdata.batch.BatchRepository;
import com.utms.masterdata.room.Room;
import com.utms.masterdata.room.RoomRepository;
import com.utms.masterdata.timeslot.SlotDefinition;
import com.utms.masterdata.timeslot.SlotDefinitionRepository;
import com.utms.scheduling.engine.enums.RecurrenceType;
import com.utms.scheduling.engine.enums.WeekGroup;
import com.utms.scheduling.engine.model.FacultyWorkloadLimits;
import com.utms.scheduling.engine.service.RecurrenceOverlapEvaluator;
import com.utms.scheduling.engine.service.RecurrenceOverlapEvaluator.SessionRecurrence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Applies the real-time conflict rules to a proposed placement against a draft's
 * occupancy (A4-16, KD-62). Reuses the SAME rule semantics as the generation engine's
 * {@code HardConstraintValidator} (HC-ENG-1..12) so the real-time catalogue cannot
 * drift from the generation-time definitions.
 *
 * <p>Recurrence-aware (KD-64): a same-slot occupant only conflicts with the proposed
 * placement if their occurring weeks overlap ({@link RecurrenceOverlapEvaluator}).
 * A weekly proposal is treated as WEEKLY (co-occurs with everything).
 *
 * <p>Coverage notes:
 * <ul>
 *   <li>Detected against real data: faculty double-booking, room double-booking,
 *       batch clash, room capacity.</li>
 *   <li>Workload (daily/weekly/consecutive) rules are emitted only when faculty limits
 *       are supplied by {@link FacultyLimitProvider}. The default provider returns
 *       none (no loaded master-data source yet — the engine's
 *       SchedulingDataLoader.loadFacultyLimits is a stub), so these degrade to no
 *       violation, mirroring the engine's CSPState (limits == null -> no violation).</li>
 *   <li>ROOM_HARD_BLOCK / FACULTY_HARD_BLOCK / TRAVEL_TIME / PREREQUISITE_SEQUENCE:
 *       NOT evaluated here (deferred detection — PD-96/PD-98/PD-99 and the room/faculty
 *       hard-block engine checks are not invoked from the real-time checker).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PlacementRuleChecker {

    private final RoomRepository roomRepository;
    private final BatchRepository batchRepository;
    private final SlotDefinitionRepository slotDefinitionRepository;
    private final RecurrenceOverlapEvaluator recurrenceOverlapEvaluator;
    private final FacultyLimitProvider facultyLimitProvider;

    /**
     * Evaluate a proposed placement against the draft occupancy. The proposed placement
     * is treated as WEEKLY recurrence for overlap purposes (real-time editor moves a
     * concrete session into a slot; fortnightly handling for the moved session itself
     * is governed by the stored session and OQ#8).
     */
    public List<ConflictDto> check(ProposedPlacementRequest placement, DraftOccupancyIndex index) {
        List<ConflictDto> conflicts = new ArrayList<>();
        SessionRecurrence proposedRecurrence = SessionRecurrence.weekly();

        List<DraftOccupancyIndex.Occupant> sameSlot =
                index.occupantsAt(placement.getDayOfWeek(), placement.getSlotDefinitionId(), placement.getSessionId());

        for (DraftOccupancyIndex.Occupant o : sameSlot) {
            // KD-64: skip occupants that can never co-occur with the proposal.
            if (!recurrenceOverlapEvaluator.everCoOccur(proposedRecurrence, toRecurrence(o))) {
                continue;
            }
            // HC-ENG-1: faculty double-booking
            if (o.facultyId() != null && o.facultyId().equals(placement.getFacultyId())) {
                conflicts.add(build(ConflictType.FACULTY_DOUBLE_BOOKING, placement, o,
                        "Faculty " + placement.getFacultyId() + " is already booked in this slot."));
            }
            // HC-ENG-2: room double-booking
            if (o.roomId() != null && o.roomId().equals(placement.getRoomId())) {
                conflicts.add(build(ConflictType.ROOM_DOUBLE_BOOKING, placement, o,
                        "Room " + placement.getRoomId() + " is already booked in this slot."));
            }
            // HC-ENG-3: batch clash (batch/section level — student-level is deferred, PD-95)
            if (o.batchId() != null && o.batchId().equals(placement.getBatchId())) {
                conflicts.add(build(ConflictType.BATCH_CLASH, placement, o,
                        "Batch " + placement.getBatchId() + " already has a session in this slot."));
            }
        }

        // HC-ENG-4: room capacity >= batch strength (real data via master-data repos).
        checkCapacity(placement, conflicts);

        // HC-ENG-11/12: faculty workload (daily/weekly/consecutive). Fires only when
        // faculty limits are available from the provider (data-pending otherwise).
        checkWorkload(placement, index, conflicts);

        return conflicts;
    }

    /**
     * Faculty workload rules (HC-ENG-11 daily/weekly, HC-ENG-12 consecutive), reusing
     * the engine's CSPState semantics: daily/weekly use {@code >=} the max, consecutive
     * uses {@code >} the max. "Consecutive" means a contiguous run of occupied slots
     * (by start-time order) that includes the proposed slot. All three fire only when
     * limits are provided by {@link FacultyLimitProvider}; otherwise they degrade to no
     * violation (matching CSPState: limits == null -> false).
     */
    private void checkWorkload(ProposedPlacementRequest placement, DraftOccupancyIndex index,
                               List<ConflictDto> conflicts) {
        Optional<FacultyWorkloadLimits> maybeLimits = facultyLimitProvider.getLimits(placement.getFacultyId());
        if (maybeLimits.isEmpty()) {
            return; // data-pending: no faculty limits loaded (see FacultyLimitProvider)
        }
        FacultyWorkloadLimits limits = maybeLimits.get();
        double proposedHours = placement.getDurationMinutes() / 60.0;

        // HC-ENG-11 daily: existing hours on the day for this faculty + the proposed hours.
        double dailyHours = proposedHours + index.occupantsOnDay(placement.getDayOfWeek(), placement.getSessionId())
                .stream()
                .filter(o -> placement.getFacultyId().equals(o.facultyId()))
                .mapToDouble(DraftOccupancyIndex.Occupant::durationHours)
                .sum();
        if (dailyHours >= limits.getMaxDailyHours()) {
            conflicts.add(workloadConflict(ConflictType.FACULTY_DAILY_HOURS, placement,
                    "Faculty daily hours " + dailyHours + " reach or exceed the maximum "
                            + limits.getMaxDailyHours() + "."));
        }

        // HC-ENG-11 weekly: existing hours across the draft for this faculty + proposed.
        double weeklyHours = proposedHours + index.allOccupants(placement.getSessionId())
                .stream()
                .filter(o -> placement.getFacultyId().equals(o.facultyId()))
                .mapToDouble(DraftOccupancyIndex.Occupant::durationHours)
                .sum();
        if (weeklyHours >= limits.getMaxWeeklyHours()) {
            conflicts.add(workloadConflict(ConflictType.FACULTY_WEEKLY_HOURS, placement,
                    "Faculty weekly hours " + weeklyHours + " reach or exceed the maximum "
                            + limits.getMaxWeeklyHours() + "."));
        }

        // HC-ENG-12 consecutive: contiguous run of this faculty's slots (by start time)
        // that includes the proposed slot. Engine uses strict '>' the max.
        double consecutiveHours = computeConsecutiveHours(placement, index, proposedHours);
        if (consecutiveHours > limits.getMaxConsecutiveHours()) {
            conflicts.add(workloadConflict(ConflictType.FACULTY_CONSECUTIVE_HOURS, placement,
                    "Faculty consecutive hours " + consecutiveHours
                            + " exceed the maximum " + limits.getMaxConsecutiveHours() + "."));
        }
    }

    /**
     * Computes the faculty's contiguous teaching-hours run that includes the proposed
     * slot. Occupied slots on the day are ordered by start time; a run is contiguous
     * when consecutive occupied slots touch (previous end == next start). Slots whose
     * start time cannot be resolved are skipped (conservative).
     */
    private double computeConsecutiveHours(ProposedPlacementRequest placement, DraftOccupancyIndex index,
                                           double proposedHours) {
        LocalTime proposedStart = slotStart(placement.getSlotDefinitionId());
        if (proposedStart == null) {
            return proposedHours; // cannot order without a start time
        }

        // Collect this faculty's occupied slots on the day (excluding the moved session),
        // plus the proposed slot, as (start, end) intervals ordered by start.
        List<LocalTime[]> intervals = new ArrayList<>();
        for (DraftOccupancyIndex.Occupant o : index.occupantsOnDay(placement.getDayOfWeek(), placement.getSessionId())) {
            if (!placement.getFacultyId().equals(o.facultyId())) {
                continue;
            }
            LocalTime start = slotStart(o.slotDefinitionId());
            if (start != null) {
                intervals.add(new LocalTime[]{start, start.plusMinutes((long) (o.durationHours() * 60))});
            }
        }
        intervals.add(new LocalTime[]{proposedStart,
                proposedStart.plusMinutes((long) (proposedHours * 60))});
        intervals.sort(Comparator.comparing(iv -> iv[0]));

        // Find the contiguous run containing the proposed slot.
        double bestRunHours = 0.0;
        double currentRunMinutes = 0.0;
        LocalTime prevEnd = null;
        boolean runIncludesProposed = false;
        for (LocalTime[] iv : intervals) {
            double durMin = java.time.Duration.between(iv[0], iv[1]).toMinutes();
            if (prevEnd != null && iv[0].equals(prevEnd)) {
                currentRunMinutes += durMin;
            } else {
                currentRunMinutes = durMin;
                runIncludesProposed = false;
            }
            if (iv[0].equals(proposedStart)) {
                runIncludesProposed = true;
            }
            if (runIncludesProposed) {
                bestRunHours = Math.max(bestRunHours, currentRunMinutes / 60.0);
            }
            prevEnd = iv[1];
        }
        // bestRunHours already includes the proposed slot (proposedStart resolved above),
        // so it is the contiguous run containing the proposal.
        return bestRunHours;
    }

    private LocalTime slotStart(Long slotDefinitionId) {
        return slotDefinitionRepository.findByIdAndDeletedAtIsNull(slotDefinitionId)
                .map(SlotDefinition::getStartTime)
                .orElse(null);
    }

    private static ConflictDto workloadConflict(ConflictType type, ProposedPlacementRequest placement,
                                                String description) {
        return ConflictDto.builder()
                .type(type)
                .involvedFacultyId(placement.getFacultyId())
                .dayOfWeek(placement.getDayOfWeek())
                .slotDefinitionId(placement.getSlotDefinitionId())
                .description(description)
                .build();
    }

    private void checkCapacity(ProposedPlacementRequest placement, List<ConflictDto> conflicts) {
        Integer capacity = roomRepository.findByIdAndDeletedAtIsNull(placement.getRoomId())
                .map(Room::getCapacity)
                .orElse(null);
        Integer strength = batchRepository.findByIdAndDeletedAtIsNull(placement.getBatchId())
                .map(Batch::getStrength)
                .orElse(null);
        if (capacity != null && strength != null && capacity < strength) {
            conflicts.add(ConflictDto.builder()
                    .type(ConflictType.ROOM_CAPACITY)
                    .involvedRoomId(placement.getRoomId())
                    .involvedBatchId(placement.getBatchId())
                    .dayOfWeek(placement.getDayOfWeek())
                    .slotDefinitionId(placement.getSlotDefinitionId())
                    .description("Room capacity " + capacity + " is less than batch strength " + strength + ".")
                    .build());
        }
    }

    private static SessionRecurrence toRecurrence(DraftOccupancyIndex.Occupant o) {
        if (o.recurrenceType() == RecurrenceType.FORTNIGHTLY) {
            WeekGroup group = o.weekGroup();
            return SessionRecurrence.fortnightly(group);
        }
        return SessionRecurrence.weekly();
    }

    private static ConflictDto build(ConflictType type, ProposedPlacementRequest placement,
                                     DraftOccupancyIndex.Occupant occupant, String description) {
        return ConflictDto.builder()
                .type(type)
                .involvedFacultyId(occupant.facultyId())
                .involvedRoomId(occupant.roomId())
                .involvedBatchId(occupant.batchId())
                .involvedSectionId(occupant.sectionId())
                .involvedSessionId(occupant.sessionId())
                .dayOfWeek(placement.getDayOfWeek())
                .slotDefinitionId(placement.getSlotDefinitionId())
                .description(description)
                .build();
    }
}
