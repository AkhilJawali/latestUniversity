package com.utms.scheduling.conflict;

import com.utms.masterdata.timeslot.SlotDefinition;
import com.utms.masterdata.timeslot.SlotDefinitionRepository;
import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.repository.ScheduledSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hydrates a {@link DraftOccupancyIndex} from the persisted sessions of a draft
 * (A4-16, design §5.3). Read-only.
 *
 * <p>Resolves each session's slot duration from its {@code SlotDefinition} so the
 * workload and consecutive-hours rules have hour data. Slot durations are looked up
 * once and cached per load call to avoid repeated queries for shared slots.
 *
 * <p>Design note (§5.3): room capacity/building is intentionally NOT resolved here.
 * Capacity is only meaningful against the PROPOSED placement's batch strength at
 * check time, so the capacity lookup lives in {@code PlacementRuleChecker} (which
 * has the proposed batch), not in this occupancy loader.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DraftOccupancyLoader {

    private final ScheduledSessionRepository sessionRepository;
    private final SlotDefinitionRepository slotDefinitionRepository;

    @Transactional(readOnly = true)
    public DraftOccupancyIndex load(Long draftId) {
        List<ScheduledSession> sessions = sessionRepository.findByDraftIdAndDeletedAtIsNull(draftId);
        DraftOccupancyIndex index = new DraftOccupancyIndex();
        Map<Long, Double> slotHoursCache = new HashMap<>();

        for (ScheduledSession s : sessions) {
            double durationHours = resolveSlotHours(s.getSlotDefinitionId(), slotHoursCache);
            index.add(new DraftOccupancyIndex.Occupant(
                    s.getId(),
                    s.getFacultyId(),
                    s.getRoomId(),
                    s.getBatchId(),
                    s.getSectionId(),
                    s.getDayOfWeek(),
                    s.getSlotDefinitionId(),
                    durationHours,
                    s.getRecurrenceType(),
                    s.getWeekGroup()));
        }
        log.debug("Loaded occupancy index for draft {}: {} sessions", draftId, sessions.size());
        return index;
    }

    private double resolveSlotHours(Long slotDefinitionId, Map<Long, Double> cache) {
        return cache.computeIfAbsent(slotDefinitionId, id -> {
            Double hours = slotDefinitionRepository.findByIdAndDeletedAtIsNull(id)
                    .map(SlotDefinition::getDurationMinutes)
                    .map(min -> min / 60.0)
                    .orElse(null);
            if (hours == null) {
                // Fallback so workload math stays defined; logged so the degrade is
                // observable rather than silently fabricating a duration.
                log.warn("Slot definition {} not found/resolvable; defaulting occupancy "
                        + "duration to 1.0h for conflict detection", id);
                return 1.0;
            }
            return hours;
        });
    }
}
