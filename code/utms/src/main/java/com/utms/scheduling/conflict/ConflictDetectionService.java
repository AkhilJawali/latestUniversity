package com.utms.scheduling.conflict;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utms.common.exception.EntityNotFoundException;
import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.entity.TimetableDraft;
import com.utms.scheduling.engine.repository.ScheduledSessionRepository;
import com.utms.scheduling.engine.repository.TimetableDraftRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for real-time conflict detection.
 * 
 * Provides two main operations:
 * - checkPlacement: Validates a proposed placement before committing
 * - checkDraft: Scans the entire draft for conflicts
 * 
 * KD-61: Uses DraftOccupancyIndex for O(1) conflict lookups.
 * KD-62: Reuses A4-11 rule definitions via PlacementRuleChecker.
 * 
 * Design: A4-16  5 ConflictDetectionService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConflictDetectionService {

    private final DraftOccupancyLoader occupancyLoader;
    private final PlacementRuleChecker ruleChecker;
    private final ScheduledSessionRepository sessionRepository;
    private final TimetableDraftRepository draftRepository;

    /**
     * Checks a proposed placement for conflicts.
     * 
     *  5.1:
     * 1. Validate draftId resolves to a non-deleted draft (else 400)
     * 2. Obtain DraftOccupancyIndex for the draft
     * 3. Exclude the session being moved (if sessionId present) from occupancy
     * 4. Run PlacementRuleChecker predicates
     * 5. Recurrence guard (KD-64)
     * 6. Return the list (empty = valid)
     * 
     * @param request the placement request
     * @return list of detected conflicts
     */
    @Transactional(readOnly = true)
    public List<ConflictDto> checkPlacement(ProposedPlacementRequest request) {
        log.debug("Checking placement for draft {}", request.getDraftId());
        
        // 1. Validate draft exists
        TimetableDraft draft = draftRepository.findByIdAndDeletedAtIsNull(request.getDraftId())
                .orElseThrow(() -> new EntityNotFoundException("TimetableDraft", request.getDraftId()));
        
        // 2. Build occupancy index for the draft
        DraftOccupancyIndex index = occupancyLoader.loadForDraft(request.getDraftId());
        
        // 3. Get existing session if moving
        ScheduledSession existingSession = null;
        if (request.getSessionId() != null) {
            existingSession = sessionRepository.findByIdAndDeletedAtIsNull(request.getSessionId())
                    .orElse(null);
        }
        
        // 4. Check all rules
        return ruleChecker.checkPlacement(request, existingSession, index);
    }

    /**
     * Checks an entire draft for conflicts.
     * 
     *  5.2: Iterate placed sessions, evaluate each against the rest via the index,
     * aggregate all conflicts.
     * 
     * @param draftId the draft ID
     * @return list of all detected conflicts
     */
    @Transactional(readOnly = true)
    public List<ConflictDto> checkDraft(Long draftId) {
        log.debug("Checking all conflicts for draft {}", draftId);
        
        // Validate draft exists
        TimetableDraft draft = draftRepository.findByIdAndDeletedAtIsNull(draftId)
                .orElseThrow(() -> new EntityNotFoundException("TimetableDraft", draftId));
        
        // Build occupancy index
        DraftOccupancyIndex index = occupancyLoader.loadForDraft(draftId);
        
        List<ConflictDto> allConflicts = new ArrayList<>();
        
        // Check each session in the draft
        for (ScheduledSession session : index.getAllSessions()) {
            if (session.getRoomId() == null || session.getDayOfWeek() == null || session.getSlotDefinitionId() == null) {
                continue; // Skip unplaced sessions
            }
            
            ProposedPlacementRequest request = ProposedPlacementRequest.builder()
                    .draftId(draftId)
                    .sessionId(session.getId())
                    .roomId(session.getRoomId())
                    .facultyId(session.getFacultyId())
                    .batchId(session.getBatchId())
                    .sectionId(session.getSectionId())
                    .dayOfWeek(session.getDayOfWeek())
                    .slotDefinitionId(session.getSlotDefinitionId())
                    .build();
            
            List<ConflictDto> conflicts = ruleChecker.checkPlacement(request, session, index);
            allConflicts.addAll(conflicts);
        }
        
        // Deduplicate conflicts (each pair reports twice)
        return deduplicateConflicts(allConflicts);
    }

    /**
     * Checks for cross-draft conflicts.
     * 
     * @param draftId the draft ID to check
     * @param otherDraftIds other published/approved drafts to check against
     * @return list of cross-draft conflicts
     */
    @Transactional(readOnly = true)
    public List<ConflictDto> checkCrossDraftConflicts(Long draftId, List<Long> otherDraftIds) {
        log.debug("Checking cross-draft conflicts for draft {} against {} other drafts", draftId, otherDraftIds.size());
        
        // Load current draft's sessions
        DraftOccupancyIndex currentIndex = occupancyLoader.loadForDraft(draftId);
        
        // Load other drafts' sessions
        DraftOccupancyIndex otherIndex = occupancyLoader.loadForDrafts(otherDraftIds);
        
        List<ConflictDto> conflicts = new ArrayList<>();
        
        for (ScheduledSession session : currentIndex.getAllSessions()) {
            if (session.getRoomId() == null || session.getDayOfWeek() == null || session.getSlotDefinitionId() == null) {
                continue;
            }
            
            // Check room conflicts with other drafts
            var otherRoomSessions = otherIndex.getSessionsByRoom(
                    session.getRoomId(),
                    session.getDayOfWeek(),
                    session.getSlotDefinitionId());
            
            for (ScheduledSession other : otherRoomSessions) {
                // Check recurrence overlap before reporting conflict
                if (hasRecurrenceOverlap(session, other)) {
                    conflicts.add(ConflictDto.from(
                            ConflictType.CROSS_DRAFT_ROOM_DOUBLE_BOOKING,
                            session.getId(),
                            session.getDayOfWeek(),
                            session.getSlotDefinitionId(),
                            session.getRoomId(),
                            null,
                            null,
                            null,
                            List.of(other.getId()),
                            "Room is used in another draft"
                    ));
                }
            }
            
            // Check faculty conflicts with other drafts
            if (session.getFacultyId() != null) {
                var otherFacultySessions = otherIndex.getSessionsByFaculty(
                        session.getFacultyId(),
                        session.getDayOfWeek(),
                        session.getSlotDefinitionId());
                
                for (ScheduledSession other : otherFacultySessions) {
                    // Check recurrence overlap before reporting conflict
                    if (hasRecurrenceOverlap(session, other)) {
                        conflicts.add(ConflictDto.from(
                                ConflictType.CROSS_DRAFT_FACULTY_DOUBLE_BOOKING,
                                session.getId(),
                                session.getDayOfWeek(),
                                session.getSlotDefinitionId(),
                                null,
                                session.getFacultyId(),
                                null,
                                null,
                                List.of(other.getId()),
                                "Faculty is assigned in another draft"
                        ));
                    }
                }
            }
        }
        
        return conflicts;
    }

    /**
     * Checks if two sessions have overlapping recurrence patterns.
     */
    private boolean hasRecurrenceOverlap(ScheduledSession session1, ScheduledSession session2) {
        // If either session has no recurrence type, they overlap (default WEEKLY)
        if (session1.getRecurrenceType() == null || session2.getRecurrenceType() == null) {
            return true;
        }
        
        // If both are WEEKLY, they always overlap
        if (session1.getRecurrenceType() == com.utms.scheduling.engine.enums.RecurrenceType.WEEKLY
                && session2.getRecurrenceType() == com.utms.scheduling.engine.enums.RecurrenceType.WEEKLY) {
            return true;
        }
        
        // If one is WEEKLY and other is FORTNIGHTLY, they overlap (WEEKLY occurs every week)
        if (session1.getRecurrenceType() == com.utms.scheduling.engine.enums.RecurrenceType.WEEKLY
                || session2.getRecurrenceType() == com.utms.scheduling.engine.enums.RecurrenceType.WEEKLY) {
            return true;
        }
        
        // Both are FORTNIGHTLY - check if same week group
        return session1.getWeekGroup() == session2.getWeekGroup();
    }

    /**
     * Deduplicates conflicts by removing duplicate pairs.
     */
    private List<ConflictDto> deduplicateConflicts(List<ConflictDto> conflicts) {
        // Use a set to track seen conflict pairs
        java.util.Set<String> seen = new java.util.HashSet<>();
        List<ConflictDto> deduped = new ArrayList<>();
        
        for (ConflictDto conflict : conflicts) {
            // Create a unique key for this conflict
            String key = conflict.getType() + ":" + 
                    (conflict.getConflictingSessionIds() != null && !conflict.getConflictingSessionIds().isEmpty()
                            ? conflict.getConflictingSessionIds().get(0)
                            : conflict.getSessionId());
            
            if (!seen.contains(key)) {
                seen.add(key);
                deduped.add(conflict);
            }
        }
        
        return deduped;
    }
}