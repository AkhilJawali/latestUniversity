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
 * <p>Design note (KD-61): The design described BitSet occupancy maps; this implementation
 * keys occupancy by (dayOfWeek, slotDefinitionId) to a list of {@link Occupant} records
 * instead. Persisted sessions carry {@code slotDefinitionId} (not a contiguous slot
 * index), and the rule checker needs per-occupant data (recurrence, section, duration)
 * that a bare BitSet cannot carry. Lookups remain O(1) per slot. This is a mechanism-level
 * refinement of KD-61, not a scope change.
 */
public class DraftOccupancyIndex {

    /**
     * A single placed session occupying a (day, slot). Immutable.
     *
     * @param sessionId        the scheduled session ID
     * @param facultyId        the faculty teaching this session
     * @param roomId           the room where this session is held
     * @param batchId          the batch attending this session
     * @param sectionId        the section (if any) for split batches
     * @param dayOfWeek        the day of week (e.g., "MONDAY")
     * @param slotDefinitionId the slot definition ID
     * @param durationHours    the duration in hours (for workload calculations)
     * @param recurrenceType   WEEKLY or FORTNIGHTLY
     * @param weekGroup        for FORTNIGHTLY: WEEK_A or WEEK_B
     */
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

    /**
     * Add a placed session to the index.
     *
     * @param occupant the session occupant to add
     */
    public void add(Occupant occupant) {
        allOccupants.add(occupant);
        bySlot.computeIfAbsent(slotKey(occupant.dayOfWeek(), occupant.slotDefinitionId()), k -> new ArrayList<>())
                .add(occupant);
    }

    /**
     * Get occupants at a given (day, slot), excluding the session being moved (if any).
     *
     * @param dayOfWeek         the day of week
     * @param slotDefinitionId  the slot definition ID
     * @param excludeSessionId  the session ID to exclude (for move operations)
     * @return list of occupants at that slot, never null
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

    /**
     * Get all occupants on a given day (used for workload/consecutive checks).
     *
     * @param dayOfWeek         the day of week
     * @param excludeSessionId  the session ID to exclude
     * @return list of occupants on that day
     */
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

    /**
     * Get all occupants in the draft (used for weekly workload and full-draft checks).
     *
     * @param excludeSessionId the session ID to exclude
     * @return list of all occupants
     */
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
