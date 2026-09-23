package com.utms.scheduling.conflict;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.enums.RecurrenceType;
import com.utms.scheduling.engine.enums.WeekGroup;
import com.utms.scheduling.engine.service.RecurrenceOverlapEvaluator;
import com.utms.scheduling.engine.service.RecurrenceOverlapEvaluator.SessionRecurrence;
import com.utms.masterdata.batch.Batch;
import com.utms.masterdata.batch.BatchRepository;
import com.utms.masterdata.room.Room;
import com.utms.masterdata.room.RoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Checks placement rules for conflict detection.
 * 
 * Reuses constraints from A4-11 HardConstraintValidator but adapted for
 * real-time conflict detection with draft-scoped occupancy index.
 * 
 * KD-62: Reuse the A4-11 rule definitions via a thin PlacementRuleChecker
 * that applies the same predicates against the DraftOccupancyIndex.
 * 
 * Design: A4-16  5 PlacementRuleChecker
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlacementRuleChecker {

    private final RoomRepository roomRepository;
    private final BatchRepository batchRepository;
    private final RecurrenceOverlapEvaluator recurrenceEvaluator;

    /**
     * Checks all hard constraints for a proposed placement.
     * 
     * @param request the proposed placement
     * @param existingSession the existing session (if moving), null for new session
     * @param index the draft occupancy index
     * @return list of detected conflicts (empty if no conflicts)
     */
    public List<ConflictDto> checkPlacement(
            ProposedPlacementRequest request,
            ScheduledSession existingSession,
            DraftOccupancyIndex index) {
        
        List<ConflictDto> conflicts = new ArrayList<>();
        
        Long sessionId = request.getSessionId(); // null for new sessions
        Long roomId = request.getRoomId();
        Long facultyId = request.getFacultyId();
        Long batchId = request.getBatchId();
        Long sectionId = request.getSectionId();
        String dayOfWeek = request.getDayOfWeek();
        Long slotDefinitionId = request.getSlotDefinitionId();
        
        // Get recurrence info from existing session (for move operations)
        RecurrenceType proposedRecurrenceType = existingSession != null 
                ? existingSession.getRecurrenceType() : null;
        WeekGroup proposedWeekGroup = existingSession != null 
                ? existingSession.getWeekGroup() : null;
        
        // 1. Room double-booking (HC-ENG-2)
        conflicts.addAll(checkRoomDoubleBooking(sessionId, roomId, dayOfWeek, slotDefinitionId, 
                proposedRecurrenceType, proposedWeekGroup, index));
        
        // 2. Faculty double-booking (HC-ENG-1)
        conflicts.addAll(checkFacultyDoubleBooking(sessionId, facultyId, dayOfWeek, slotDefinitionId, 
                proposedRecurrenceType, proposedWeekGroup, index));
        
        // 3. Batch clash (HC-ENG-3)
        conflicts.addAll(checkBatchClash(sessionId, batchId, dayOfWeek, slotDefinitionId, 
                proposedRecurrenceType, proposedWeekGroup, index));
        
        // 4. Room capacity (HC-ENG-4)
        conflicts.addAll(checkRoomCapacity(roomId, batchId));
        
        return conflicts;
    }

    /**
     * Checks room double-booking (HC-ENG-2).
     */
    private List<ConflictDto> checkRoomDoubleBooking(
            Long sessionId,
            Long roomId,
            String dayOfWeek,
            Long slotDefinitionId,
            RecurrenceType proposedRecurrenceType,
            WeekGroup proposedWeekGroup,
            DraftOccupancyIndex index) {
        
        List<ConflictDto> conflicts = new ArrayList<>();
        
        var occupyingSessions = index.getSessionsByRoom(roomId, dayOfWeek, slotDefinitionId);
        
        for (ScheduledSession occupying : occupyingSessions) {
            // Skip self when moving an existing session
            if (sessionId != null && occupying.getId().equals(sessionId)) {
                continue;
            }
            
            // KD-64: Recurrence-aware overlap check
            if (hasRecurrenceOverlap(proposedRecurrenceType, proposedWeekGroup, 
                    occupying.getRecurrenceType(), occupying.getWeekGroup())) {
                conflicts.add(ConflictDto.from(
                        ConflictType.ROOM_DOUBLE_BOOKING,
                        sessionId,
                        dayOfWeek,
                        slotDefinitionId,
                        roomId,
                        null,
                        null,
                        null,
                        List.of(occupying.getId()),
                        "Room is already occupied by session " + occupying.getId()
                ));
            }
        }
        
        return conflicts;
    }

    /**
     * Checks faculty double-booking (HC-ENG-1).
     */
    private List<ConflictDto> checkFacultyDoubleBooking(
            Long sessionId,
            Long facultyId,
            String dayOfWeek,
            Long slotDefinitionId,
            RecurrenceType proposedRecurrenceType,
            WeekGroup proposedWeekGroup,
            DraftOccupancyIndex index) {
        
        List<ConflictDto> conflicts = new ArrayList<>();
        
        var occupyingSessions = index.getSessionsByFaculty(facultyId, dayOfWeek, slotDefinitionId);
        
        for (ScheduledSession occupying : occupyingSessions) {
            // Skip self when moving an existing session
            if (sessionId != null && occupying.getId().equals(sessionId)) {
                continue;
            }
            
            // KD-64: Recurrence-aware overlap check
            if (hasRecurrenceOverlap(proposedRecurrenceType, proposedWeekGroup, 
                    occupying.getRecurrenceType(), occupying.getWeekGroup())) {
                conflicts.add(ConflictDto.from(
                        ConflictType.FACULTY_DOUBLE_BOOKING,
                        sessionId,
                        dayOfWeek,
                        slotDefinitionId,
                        null,
                        facultyId,
                        null,
                        null,
                        List.of(occupying.getId()),
                        "Faculty is already assigned to session " + occupying.getId()
                ));
            }
        }
        
        return conflicts;
    }

    /**
     * Checks batch clash (HC-ENG-3).
     */
    private List<ConflictDto> checkBatchClash(
            Long sessionId,
            Long batchId,
            String dayOfWeek,
            Long slotDefinitionId,
            RecurrenceType proposedRecurrenceType,
            WeekGroup proposedWeekGroup,
            DraftOccupancyIndex index) {
        
        List<ConflictDto> conflicts = new ArrayList<>();
        
        var occupyingSessions = index.getSessionsByBatch(batchId, dayOfWeek, slotDefinitionId);
        
        for (ScheduledSession occupying : occupyingSessions) {
            // Skip self when moving an existing session
            if (sessionId != null && occupying.getId().equals(sessionId)) {
                continue;
            }
            
            // KD-64: Recurrence-aware overlap check
            if (hasRecurrenceOverlap(proposedRecurrenceType, proposedWeekGroup, 
                    occupying.getRecurrenceType(), occupying.getWeekGroup())) {
                conflicts.add(ConflictDto.from(
                        ConflictType.BATCH_CLASH,
                        sessionId,
                        dayOfWeek,
                        slotDefinitionId,
                        null,
                        null,
                        batchId,
                        null,
                        List.of(occupying.getId()),
                        "Batch has a clash with session " + occupying.getId()
                ));
            }
        }
        
        return conflicts;
    }

    /**
     * Checks room capacity (HC-ENG-4).
     */
    private List<ConflictDto> checkRoomCapacity(Long roomId, Long batchId) {
        List<ConflictDto> conflicts = new ArrayList<>();
        
        Room room = roomRepository.findByIdAndDeletedAtIsNull(roomId).orElse(null);
        if (room == null) {
            return conflicts; // Room not found - let other validation catch this
        }
        
        Batch batch = batchRepository.findByIdAndDeletedAtIsNull(batchId).orElse(null);
        if (batch == null) {
            return conflicts; // Batch not found - let other validation catch this
        }
        
        int roomCapacity = room.getCapacity() != null ? room.getCapacity() : 0;
        int batchStrength = batch.getStrength() != null ? batch.getStrength() : 0;
        
        if (batchStrength > roomCapacity) {
            conflicts.add(ConflictDto.from(
                    ConflictType.ROOM_CAPACITY_EXCEEDED,
                    null,
                    null,
                    null,
                    roomId,
                    null,
                    batchId,
                    null,
                    List.of(),
                    "Room capacity (" + roomCapacity + ") exceeded by batch strength (" + batchStrength + ")"
            ));
        }
        
        return conflicts;
    }

    /**
     * Checks if two sessions have overlapping recurrence patterns.
     * KD-64: Recurrence-aware conflict detection using RecurrenceOverlapEvaluator.
     * 
     * @param proposedRecurrenceType the proposed session's recurrence type (null = WEEKLY)
     * @param proposedWeekGroup the proposed session's week group
     * @param existingRecurrenceType the existing session's recurrence type
     * @param existingWeekGroup the existing session's week group
     * @return true if the sessions can co-occur in the same week
     */
    private boolean hasRecurrenceOverlap(
            RecurrenceType proposedRecurrenceType,
            WeekGroup proposedWeekGroup,
            RecurrenceType existingRecurrenceType,
            WeekGroup existingWeekGroup) {
        
        // Build proposed session's recurrence (default to WEEKLY if not fortnightly)
        SessionRecurrence proposed;
        if (proposedRecurrenceType == RecurrenceType.FORTNIGHTLY && proposedWeekGroup != null) {
            proposed = SessionRecurrence.fortnightly(proposedWeekGroup);
        } else {
            proposed = SessionRecurrence.weekly();
        }
        
        // Build existing session's recurrence
        SessionRecurrence existing;
        if (existingRecurrenceType == RecurrenceType.FORTNIGHTLY && existingWeekGroup != null) {
            existing = SessionRecurrence.fortnightly(existingWeekGroup);
        } else {
            existing = SessionRecurrence.weekly();
        }
        
        return recurrenceEvaluator.everCoOccur(proposed, existing);
    }
}