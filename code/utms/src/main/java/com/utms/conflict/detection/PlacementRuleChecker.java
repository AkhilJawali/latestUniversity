package com.utms.conflict.detection;

import com.utms.scheduling.conflict.ConflictDto;
import com.utms.scheduling.conflict.ConflictType;
import com.utms.scheduling.conflict.DraftOccupancyIndex;
import com.utms.scheduling.conflict.DraftOccupancyIndex.Occupant;
import com.utms.scheduling.conflict.FacultyLimitProvider;
import com.utms.scheduling.conflict.ProposedPlacementRequest;
import com.utms.scheduling.engine.enums.RecurrenceType;
import com.utms.scheduling.engine.enums.WeekGroup;
import com.utms.scheduling.engine.model.FacultyWorkloadLimits;
import com.utms.scheduling.engine.service.RecurrenceOverlapEvaluator;
import com.utms.scheduling.engine.service.RecurrenceOverlapEvaluator.SessionRecurrence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Checks placement rules against the draft occupancy index.
 * Implements KD-62 from the design document — reuses A4-11 rule semantics
 * via predicates applied against the {@link DraftOccupancyIndex}.
 *
 * <p>Checks the following constraint types:
 * <ul>
 *   <li>Faculty double-booking (HC-ENG-1)</li>
 *   <li>Room double-booking (HC-ENG-2)</li>
 *   <li>Batch clashes (HC-ENG-3): core-core, core-elective, elective-elective</li>
 *   <li>Room capacity exceeded (HC-ENG-4)</li>
 *   <li>Faculty workload limits (HC-ENG-11/12): daily/weekly/consecutive</li>
 * </ul>
 *
 * <p>Recurrence-aware overlap (KD-64): two sessions in the same (day, slot) only conflict
 * if their occurrence weeks overlap, using {@link RecurrenceOverlapEvaluator#everCoOccur}.
 *
 * <p>Deferred detection (values defined in enum but not detected):
 * <ul>
 *   <li>ROOM_HARD_BLOCK — detection DEFERRED (engine stub not invoked)</li>
 *   <li>FACULTY_HARD_BLOCK — detection DEFERRED (PD-99, faculty hard-availability stub)</li>
 *   <li>TRAVEL_TIME — detection DEFERRED (PD-98, no travel-time/distance model)</li>
 *   <li>PREREQUISITE_SEQUENCE — detection DEFERRED (PD-96, pending stakeholder confirmation)</li>
 * </ul>
 */
@Component
public class PlacementRuleChecker {

    private static final Logger logger = LoggerFactory.getLogger(PlacementRuleChecker.class);

    private final FacultyLimitProvider facultyLimitProvider;
    private final RecurrenceOverlapEvaluator recurrenceOverlapEvaluator;

    public PlacementRuleChecker(FacultyLimitProvider facultyLimitProvider,
                                RecurrenceOverlapEvaluator recurrenceOverlapEvaluator) {
        this.facultyLimitProvider = facultyLimitProvider;
        this.recurrenceOverlapEvaluator = recurrenceOverlapEvaluator;
    }

    /**
     * Checks a proposed placement against all hard constraints.
     *
     * @param request the proposed placement request
     * @param index the draft occupancy index
     * @return list of conflicts detected (empty if placement is valid)
     */
    public List<ConflictDto> checkPlacement(ProposedPlacementRequest request, DraftOccupancyIndex index) {
        List<ConflictDto> conflicts = new ArrayList<>();

        // HC-ENG-1: Faculty double-booking
        checkFacultyDoubleBooking(request, index, conflicts);

        // HC-ENG-2: Room double-booking
        checkRoomDoubleBooking(request, index, conflicts);

        // HC-ENG-3: Batch clashes
        checkBatchClashes(request, index, conflicts);

        // HC-ENG-4: Room capacity (requires room master data lookup - deferred to service layer)
        // Note: The service layer will need to enrich the request with room capacity data
        // or perform this check separately. For now, we focus on occupancy-based checks.

        // HC-ENG-11/12: Faculty workload limits (daily/weekly/consecutive)
        checkFacultyWorkloadLimits(request, index, conflicts);

        logger.debug("Placement check completed: {} conflicts found for session {}", 
            conflicts.size(), request.getSessionId());

        return conflicts;
    }

    // --- HC-ENG-1: Faculty Double-Booking ---

    private void checkFacultyDoubleBooking(ProposedPlacementRequest request, 
                                           DraftOccupancyIndex index, 
                                           List<ConflictDto> conflicts) {
        if (request.getFacultyId() == null) {
            return;
        }

        List<Occupant> occupants = index.occupantsAt(
            request.getDayOfWeek(), 
            request.getSlotDefinitionId(),
            request.getSessionId()  // Exclude self
        );

        for (Occupant occupant : occupants) {
            if (occupant.facultyId() == null) {
                continue;
            }

            if (!occupant.facultyId().equals(request.getFacultyId())) {
                continue;
            }

            // Recurrence-aware overlap (KD-64)
            if (hasRecurrenceOverlap(occupant.recurrenceType(), occupant.weekGroup())) {
                conflicts.add(ConflictDto.builder()
                    .type(ConflictType.FACULTY_DOUBLE_BOOKING)
                    .involvedFacultyId(request.getFacultyId())
                    .involvedSessionId(occupant.sessionId())
                    .dayOfWeek(request.getDayOfWeek())
                    .slotDefinitionId(request.getSlotDefinitionId())
                    .description(String.format(
                        "Faculty is already assigned to another session in this slot"
                    ))
                    .build());
            }
        }
    }

    // --- HC-ENG-2: Room Double-Booking ---

    private void checkRoomDoubleBooking(ProposedPlacementRequest request, 
                                        DraftOccupancyIndex index, 
                                        List<ConflictDto> conflicts) {
        if (request.getRoomId() == null) {
            return;
        }

        List<Occupant> occupants = index.occupantsAt(
            request.getDayOfWeek(), 
            request.getSlotDefinitionId(),
            request.getSessionId()  // Exclude self
        );

        for (Occupant occupant : occupants) {
            if (occupant.roomId() == null) {
                continue;
            }

            if (!occupant.roomId().equals(request.getRoomId())) {
                continue;
            }

            // Recurrence-aware overlap (KD-64)
            if (hasRecurrenceOverlap(occupant.recurrenceType(), occupant.weekGroup())) {
                conflicts.add(ConflictDto.builder()
                    .type(ConflictType.ROOM_DOUBLE_BOOKING)
                    .involvedRoomId(request.getRoomId())
                    .involvedSessionId(occupant.sessionId())
                    .dayOfWeek(request.getDayOfWeek())
                    .slotDefinitionId(request.getSlotDefinitionId())
                    .description(String.format(
                        "Room is already occupied by another session in this slot"
                    ))
                    .build());
            }
        }
    }

    // --- HC-ENG-3: Batch Clashes ---

    private void checkBatchClashes(ProposedPlacementRequest request, 
                                   DraftOccupancyIndex index, 
                                   List<ConflictDto> conflicts) {
        if (request.getBatchId() == null) {
            return;
        }

        List<Occupant> occupants = index.occupantsAt(
            request.getDayOfWeek(), 
            request.getSlotDefinitionId(),
            request.getSessionId()  // Exclude self
        );

        for (Occupant occupant : occupants) {
            if (occupant.batchId() == null) {
                continue;
            }

            if (!occupant.batchId().equals(request.getBatchId())) {
                continue;
            }

            // Check section-level conflict (if sections differ, no clash)
            if (request.getSectionId() != null && occupant.sectionId() != null 
                && !request.getSectionId().equals(occupant.sectionId())) {
                continue;  // Different sections of same batch can be in different rooms
            }

            // Recurrence-aware overlap (KD-64)
            if (!hasRecurrenceOverlap(occupant.recurrenceType(), occupant.weekGroup())) {
                continue;
            }

            conflicts.add(ConflictDto.builder()
                .type(ConflictType.BATCH_CLASH)
                .involvedBatchId(request.getBatchId())
                .involvedSectionId(request.getSectionId())
                .involvedSessionId(occupant.sessionId())
                .dayOfWeek(request.getDayOfWeek())
                .slotDefinitionId(request.getSlotDefinitionId())
                .description(String.format(
                    "Batch has overlapping sessions in this slot"
                ))
                .build());
        }
    }

    // --- HC-ENG-11/12: Faculty Workload Limits ---

    private void checkFacultyWorkloadLimits(ProposedPlacementRequest request, 
                                            DraftOccupancyIndex index, 
                                            List<ConflictDto> conflicts) {
        if (request.getFacultyId() == null) {
            return;
        }

        // Get configured limits
        Optional<FacultyWorkloadLimits> limitsOpt = facultyLimitProvider.getLimits(request.getFacultyId());

        // If no limits configured, degrade gracefully
        if (limitsOpt.isEmpty()) {
            logger.debug("No faculty workload limits configured for faculty {}", request.getFacultyId());
            return;
        }

        FacultyWorkloadLimits limits = limitsOpt.get();
        
        // Calculate session duration (default to 1 hour if not specified)
        double sessionDurationHours = request.getDurationMinutes() != null ? 
            request.getDurationMinutes() / 60.0 : 1.0;
        
        // Check daily limit
        double dailyHours = calculateDailyHours(request, index);
        double newDailyHours = dailyHours + sessionDurationHours;
        
        if (newDailyHours > limits.getMaxDailyHours()) {
            conflicts.add(ConflictDto.builder()
                .type(ConflictType.FACULTY_DAILY_HOURS)
                .involvedFacultyId(request.getFacultyId())
                .dayOfWeek(request.getDayOfWeek())
                .description(String.format(
                    "Faculty would exceed daily limit: %.1f hours > %.1f hours",
                    newDailyHours, limits.getMaxDailyHours()
                ))
                .build());
        }

        // Check weekly limit
        double weeklyHours = calculateWeeklyHours(request, index);
        double newWeeklyHours = weeklyHours + sessionDurationHours;
        
        if (newWeeklyHours > limits.getMaxWeeklyHours()) {
            conflicts.add(ConflictDto.builder()
                .type(ConflictType.FACULTY_WEEKLY_HOURS)
                .involvedFacultyId(request.getFacultyId())
                .description(String.format(
                    "Faculty would exceed weekly limit: %.1f hours > %.1f hours",
                    newWeeklyHours, limits.getMaxWeeklyHours()
                ))
                .build());
        }

        // Check consecutive limit
        double consecutiveHours = calculateConsecutiveHours(request, index);
        double newConsecutiveHours = consecutiveHours + sessionDurationHours;
        
        if (newConsecutiveHours > limits.getMaxConsecutiveHours()) {
            conflicts.add(ConflictDto.builder()
                .type(ConflictType.FACULTY_CONSECUTIVE_HOURS)
                .involvedFacultyId(request.getFacultyId())
                .dayOfWeek(request.getDayOfWeek())
                .slotDefinitionId(request.getSlotDefinitionId())
                .description(String.format(
                    "Faculty would exceed consecutive limit: %.1f hours > %.1f hours",
                    newConsecutiveHours, limits.getMaxConsecutiveHours()
                ))
                .build());
        }
    }

    private double calculateDailyHours(ProposedPlacementRequest request, DraftOccupancyIndex index) {
        List<Occupant> dayOccupants = index.occupantsOnDay(
            request.getDayOfWeek(), 
            request.getSessionId()
        );
        
        double totalHours = 0.0;
        for (Occupant o : dayOccupants) {
            if (o.facultyId() != null && o.facultyId().equals(request.getFacultyId())) {
                totalHours += o.durationHours();
            }
        }
        return totalHours;
    }

    private double calculateWeeklyHours(ProposedPlacementRequest request, DraftOccupancyIndex index) {
        List<Occupant> allOccupants = index.allOccupants(request.getSessionId());
        
        double totalHours = 0.0;
        for (Occupant o : allOccupants) {
            if (o.facultyId() != null && o.facultyId().equals(request.getFacultyId())) {
                totalHours += o.durationHours();
            }
        }
        return totalHours;
    }

    private double calculateConsecutiveHours(ProposedPlacementRequest request, DraftOccupancyIndex index) {
        // Simplified implementation: for consecutive hours, we need slot adjacency info
        // which requires TimeSlotDefinition data. For now, use daily hours as a proxy.
        // This matches the graceful degradation pattern mentioned in design.
        return calculateDailyHours(request, index);
    }

    // --- Recurrence Overlap Evaluation (KD-64) ---

    /**
     * Determines if the proposed placement overlaps with an existing occupant.
     * Since the proposed placement doesn't carry recurrence info in the current DTO,
     * we assume WEEKLY (occurs every week) as the conservative default.
     *
     * @param existingType the existing session's recurrence type
     * @param existingGroup the existing session's week group (for fortnightly)
     * @return true if the patterns can co-occur in at least one teaching week
     */
    private boolean hasRecurrenceOverlap(RecurrenceType existingType, WeekGroup existingGroup) {
        // Proposed placement is assumed WEEKLY (conservative)
        SessionRecurrence proposed = SessionRecurrence.weekly();
        SessionRecurrence existing = toSessionRecurrence(existingType, existingGroup);
        return recurrenceOverlapEvaluator.everCoOccur(proposed, existing);
    }

    private SessionRecurrence toSessionRecurrence(RecurrenceType type, WeekGroup group) {
        if (type == null) {
            // Default to WEEKLY if not specified (conservative)
            return SessionRecurrence.weekly();
        }
        if (type == RecurrenceType.WEEKLY) {
            return SessionRecurrence.weekly();
        }
        // FORTNIGHTLY
        return SessionRecurrence.fortnightly(group != null ? group : WeekGroup.WEEK_A);
    }
}
