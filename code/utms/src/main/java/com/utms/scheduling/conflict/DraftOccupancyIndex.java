package com.utms.scheduling.conflict;

import com.utms.scheduling.engine.enums.RecurrenceType;
import com.utms.scheduling.engine.enums.WeekGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draft-scoped occupancy index for real-time conflict detection (A4-16, KD-61).
 *
 * <p>Built once per draft from persisted {@code ScheduledSession} rows and used for
 * O(1)-per-slot occupancy lookups, independent of the generation engine's
 * {@code CSPState} (which is coupled to a generation run). Occupancy is keyed by
 * {@code (dayOfWeek, slotDefinitionId)} and stores the placed sessions occupying that
 * slot so the rule checker can apply recurrence-aware and capacity-aware logic.
 *
 * <p>Design note: KD-61 described BitSet occupancy maps; this implementation keys
 * occupancy by (dayOfWeek, slotDefinitionId) to a list of {@link Occupant} records
 * instead. Persisted sessions carry {@code slotDefinitionId} (not a contiguous slot
 * index), and the rule checker needs per-occupant data (recurrence, section,
 * duration) that a bare BitSet cannot carry. Lookups remain O(1) per slot. This is a
 * mechanism-level refinement of KD-61, not a scope change.
 */
public class DraftOccupancyIndex {

    /** A single placed session occupying a (day, slot). Immutable. */
    public record Occupant(
            Long sessionId,
            Long facultyId,
            Long roomId,
            Long batchId,
            Long sectionId,
            String dayOfWeek,
            Long slotDefinitionId,
            double durationHours,
            RecurrenceType recurrenceType,
            WeekGroup weekGroup) {
    }

    private final Map<String, List<Occupant>> bySlot = new HashMap<>();
    private final List<Occupant> allOccupants = new ArrayList<>();

    private static String slotKey(String dayOfWeek, Long slotDefinitionId) {
        return dayOfWeek + "#" + slotDefinitionId;
    }

    /** Add a placed session to the index. */
    public void add(Occupant occupant) {
        allOccupants.add(occupant);
        bySlot.computeIfAbsent(slotKey(occupant.dayOfWeek(), occupant.slotDefinitionId()), k -> new ArrayList<>())
                .add(occupant);
    }

    /**
     * Occupants at a given (day, slot), excluding the session being moved (if any).
     * Never returns null.
     */
    public List<Occupant> occupantsAt(String dayOfWeek, Long slotDefinitionId, Long excludeSessionId) {
        List<Occupant> raw = bySlot.getOrDefault(slotKey(dayOfWeek, slotDefinitionId), List.of());
        if (excludeSessionId == null) {
            return new ArrayList<>(raw);
        }
        List<Occupant> filtered = new ArrayList<>(raw.size());
        for (Occupant o : raw) {
            if (!excludeSessionId.equals(o.sessionId())) {
                filtered.add(o);
            }
        }
        return filtered;
    }

    /** All occupants on a given day (used for workload/consecutive checks). */
    public List<Occupant> occupantsOnDay(String dayOfWeek, Long excludeSessionId) {
        List<Occupant> result = new ArrayList<>();
        for (Occupant o : allOccupants) {
            if (o.dayOfWeek().equals(dayOfWeek)
                    && (excludeSessionId == null || !excludeSessionId.equals(o.sessionId()))) {
                result.add(o);
            }
        }
        return result;
    }

    /** All occupants in the draft (used for weekly workload and full-draft checks). */
    public List<Occupant> allOccupants(Long excludeSessionId) {
        if (excludeSessionId == null) {
            return new ArrayList<>(allOccupants);
        }
        List<Occupant> result = new ArrayList<>(allOccupants.size());
        for (Occupant o : allOccupants) {
            if (!excludeSessionId.equals(o.sessionId())) {
                result.add(o);
            }
        }
        return result;
    }
}
