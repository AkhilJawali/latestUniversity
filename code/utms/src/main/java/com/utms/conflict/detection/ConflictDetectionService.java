package com.utms.conflict.detection;

import com.utms.common.exception.EntityNotFoundException;
import com.utms.scheduling.conflict.ConflictDto;
import com.utms.scheduling.conflict.DraftOccupancyIndex;
import com.utms.scheduling.conflict.DraftOccupancyIndex.Occupant;
import com.utms.scheduling.conflict.DraftOccupancyLoader;
import com.utms.scheduling.conflict.ProposedPlacementRequest;
import com.utms.scheduling.engine.entity.TimetableDraft;
import com.utms.scheduling.engine.repository.TimetableDraftRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Real-time conflict detection service (A4-16, design FR-1/FR-2/FR-7).
 *
 * <p>Evaluates a proposed session placement (or a whole draft) against a timetable
 * draft's current occupancy and returns a structured list of conflicts within a
 * &lt; 2s SLA.
 *
 * <p>This service uses a draft-scoped occupancy index built from persisted
 * {@code ScheduledSession} rows, separate from the generation engine's
 * {@code CSPState} (which is coupled to a generation run).
 *
 * <p>Design references:
 * <ul>
 *   <li>FR-1: Single placement check</li>
 *   <li>FR-2: Full-draft check</li>
 *   <li>FR-5: &lt; 2s SLA via O(1) occupancy lookups</li>
 *   <li>FR-7: No false negatives (reuses A4-11 rule semantics)</li>
 * </ul>
 */
@Service
public class ConflictDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(ConflictDetectionService.class);

    private final TimetableDraftRepository draftRepository;
    private final PlacementRuleChecker ruleChecker;
    private final DraftOccupancyLoader occupancyLoader;

    public ConflictDetectionService(TimetableDraftRepository draftRepository,
                                    PlacementRuleChecker ruleChecker,
                                    DraftOccupancyLoader occupancyLoader) {
        this.draftRepository = draftRepository;
        this.ruleChecker = ruleChecker;
        this.occupancyLoader = occupancyLoader;
    }

    /**
     * Check a single proposed placement for conflicts (FR-1).
     *
     * <p>Steps:
     * <ol>
     *   <li>Validate draftId resolves to a non-deleted draft (else 400 — no internals)</li>
     *   <li>Obtain the DraftOccupancyIndex for the draft (cached; build via loader on miss)</li>
     *   <li>Exclude the session being moved (if sessionId present) from occupancy</li>
     *   <li>Run PlacementRuleChecker predicates</li>
     *   <li>Return the list (empty = valid)</li>
     * </ol>
     *
     * @param draftId the timetable draft ID
     * @param request the proposed placement
     * @return list of conflicts detected (empty if placement is valid)
     */
    public List<ConflictDto> checkPlacement(Long draftId, ProposedPlacementRequest request) {
        logger.debug("Checking placement for draft {}, session {}", draftId, request.getSessionId());

        // Validate draft exists and is not deleted (AC8)
        requireDraft(draftId);

        // Load the occupancy index for this draft
        DraftOccupancyIndex index = occupancyLoader.load(draftId);

        // Run rule checks
        List<ConflictDto> conflicts = ruleChecker.checkPlacement(request, index);

        logger.info("Placement check completed: {} conflicts found for draft {} session {}", 
            conflicts.size(), draftId, request.getSessionId());

        return conflicts;
    }

    /**
     * Check all placed sessions in a draft for conflicts (FR-2, AC9).
     *
     * <p>Iterates all placed sessions, evaluates each against the rest via the index,
     * and aggregates all conflicts. The draft check runs within the same &lt; 2s SLA
     * as single-placement checks due to O(1) occupancy lookups.
     *
     * @param draftId the timetable draft ID
     * @return list of all conflicts found in the draft
     */
    public List<ConflictDto> checkDraft(Long draftId) {
        logger.debug("Checking full draft {}", draftId);

        // Validate draft exists and is not deleted (AC8)
        requireDraft(draftId);

        // Load the occupancy index for this draft
        DraftOccupancyIndex index = occupancyLoader.load(draftId);

        List<ConflictDto> allConflicts = new ArrayList<>();

        // Iterate all placed sessions and check each against the index
        List<Occupant> allOccupants = index.allOccupants(null);
        
        for (Occupant occupant : allOccupants) {
            // Create a placement request from this occupant
            ProposedPlacementRequest request = ProposedPlacementRequest.builder()
                .facultyId(occupant.facultyId())
                .roomId(occupant.roomId())
                .batchId(occupant.batchId())
                .sectionId(occupant.sectionId())
                .dayOfWeek(occupant.dayOfWeek())
                .slotDefinitionId(occupant.slotDefinitionId())
                .durationMinutes((int) (occupant.durationHours() * 60))
                .sessionId(occupant.sessionId())
                .build();

            // Check this placement against the index (will exclude self)
            List<ConflictDto> sessionConflicts = ruleChecker.checkPlacement(request, index);
            
            // Add all conflicts (the checker excludes self-reference)
            allConflicts.addAll(sessionConflicts);
        }

        // Deduplicate conflicts (each pair will be detected twice, once from each side)
        List<ConflictDto> deduplicated = deduplicateConflicts(allConflicts);

        logger.info("Full draft check completed: {} unique conflicts found for draft {}", 
            deduplicated.size(), draftId);

        return deduplicated;
    }

    /**
     * Remove duplicate conflicts (same conflict detected from both sides).
     *
     * <p>When checking a full draft, a faculty double-booking between sessions A and B
     * will be detected both when checking A (finds B) and when checking B (finds A).
     * This method keeps only one copy of each conflict.
     *
     * @param conflicts the raw conflict list
     * @return deduplicated conflict list
     */
    private List<ConflictDto> deduplicateConflicts(List<ConflictDto> conflicts) {
        List<ConflictDto> deduplicated = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (ConflictDto conflict : conflicts) {
            // Create a unique key for this conflict
            String key = conflictKey(conflict);
            
            if (!seen.contains(key)) {
                seen.add(key);
                deduplicated.add(conflict);
            }
        }

        return deduplicated;
    }

    /**
     * Create a unique key for a conflict to enable deduplication.
     *
     * <p>Two conflicts are considered the same if they have the same type, entities,
     * and location, regardless of which session was being checked.
     */
    private String conflictKey(ConflictDto conflict) {
        StringBuilder sb = new StringBuilder();
        sb.append(conflict.getType().name());
        
        // Sort entity IDs to ensure consistent keys regardless of which side detected it
        List<Long> entityIds = new ArrayList<>();
        if (conflict.getInvolvedFacultyId() != null) {
            entityIds.add(conflict.getInvolvedFacultyId());
        }
        if (conflict.getInvolvedRoomId() != null) {
            entityIds.add(conflict.getInvolvedRoomId());
        }
        if (conflict.getInvolvedBatchId() != null) {
            entityIds.add(conflict.getInvolvedBatchId());
        }
        if (conflict.getInvolvedSessionId() != null) {
            entityIds.add(conflict.getInvolvedSessionId());
        }
        
        Collections.sort(entityIds);
        sb.append(entityIds.toString());
        
        sb.append(conflict.getDayOfWeek());
        sb.append(conflict.getSlotDefinitionId());
        
        return sb.toString();
    }

    /**
     * Validate that a draft exists and is not deleted (AC8).
     *
     * <p>Throws EntityNotFoundException if the draft does not exist or has been soft-deleted.
     * This prevents occupancy loading for nonexistent drafts and ensures consistent error handling.
     *
     * @param draftId the draft ID to validate
     * @throws EntityNotFoundException if draft not found or deleted
     */
    private void requireDraft(Long draftId) {
        Optional<TimetableDraft> draft = draftRepository.findByIdAndDeletedAtIsNull(draftId);
        if (draft.isEmpty()) {
            throw new EntityNotFoundException("TimetableDraft", draftId);
        }
    }
}
