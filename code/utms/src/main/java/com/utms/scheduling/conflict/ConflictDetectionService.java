package com.utms.scheduling.conflict;

import com.utms.common.exception.EntityNotFoundException;
import com.utms.scheduling.engine.repository.TimetableDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Real-time conflict detection service (A4-16, design §5). Evaluates a proposed
 * placement (or a whole draft) against the draft's current occupancy and returns the
 * conflicts it would introduce. Reuses the A4-11 rule semantics via
 * {@link PlacementRuleChecker}; occupancy is a draft-scoped index built from persisted
 * sessions ({@link DraftOccupancyLoader}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConflictDetectionService {

    private final TimetableDraftRepository draftRepository;
    private final DraftOccupancyLoader occupancyLoader;
    private final PlacementRuleChecker ruleChecker;

    /**
     * FR-1: check a single proposed placement against the draft's occupancy.
     *
     * @throws EntityNotFoundException if the draft does not exist / is deleted
     */
    @Transactional(readOnly = true)
    public List<ConflictDto> checkPlacement(Long draftId, ProposedPlacementRequest placement) {
        requireDraft(draftId);
        DraftOccupancyIndex index = occupancyLoader.load(draftId);
        List<ConflictDto> conflicts = ruleChecker.check(placement, index);
        log.debug("Placement check for draft {}: {} conflict(s)", draftId, conflicts.size());
        return conflicts;
    }

    /**
     * FR-2: check every placed session in the draft against the rest, aggregating all
     * conflicts. Each session is evaluated as if being (re)placed at its own slot, with
     * itself excluded from occupancy so it is not reported as clashing with itself.
     *
     * <p>Note: conflicts are reported per session (from each session's own
     * perspective), which is what the editor highlights. A pairwise clash between two
     * sessions therefore appears once per involved session (from each side). This is
     * intentional for the editor's per-session highlighting; a de-duplicated summary
     * view, if needed, is a consumer-side concern (A4-15).
     */
    @Transactional(readOnly = true)
    public List<ConflictDto> checkDraft(Long draftId) {
        requireDraft(draftId);
        DraftOccupancyIndex index = occupancyLoader.load(draftId);

        List<ConflictDto> all = new ArrayList<>();
        for (DraftOccupancyIndex.Occupant o : index.allOccupants(null)) {
            ProposedPlacementRequest asPlacement = ProposedPlacementRequest.builder()
                    .facultyId(o.facultyId())
                    .roomId(o.roomId())
                    .batchId(o.batchId())
                    .sectionId(o.sectionId())
                    .dayOfWeek(o.dayOfWeek())
                    .slotDefinitionId(o.slotDefinitionId())
                    .durationMinutes((int) Math.round(o.durationHours() * 60))
                    .sessionId(o.sessionId())
                    .build();
            all.addAll(ruleChecker.check(asPlacement, index));
        }
        log.debug("Full-draft check for draft {}: {} conflict(s)", draftId, all.size());
        return all;
    }

    private void requireDraft(Long draftId) {
        draftRepository.findByIdAndDeletedAtIsNull(draftId)
                .orElseThrow(() -> new EntityNotFoundException("TimetableDraft", draftId));
    }
}
