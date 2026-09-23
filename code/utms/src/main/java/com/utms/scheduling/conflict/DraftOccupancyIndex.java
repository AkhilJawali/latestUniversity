package com.utms.scheduling.conflict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.enums.RecurrenceType;
import com.utms.scheduling.engine.enums.WeekGroup;

import lombok.Getter;

/**
 * In-memory index of draft occupancy for O(1) conflict lookups.
 * 
 * Provides fast lookups by room+day+slot, faculty+day+slot, and batch+day+slot.
 * Used by ConflictDetectionService for real-time conflict detection.
 * 
 * KD-61: Draft-scoped occupancy index (BitSet maps for faculty/room/batch,
 * keyed by day×slot), built from persisted ScheduledSession rows.
 * 
 * Design: A4-16 §5.3 DraftOccupancyLoader
 */
public class DraftOccupancyIndex {

    /** roomDaySlot -> sessions */
    @Getter
    private final Map<String, List<ScheduledSession>> roomIndex = new HashMap<>();

    /** facultyDaySlot -> sessions */
    @Getter
    private final Map<String, List<ScheduledSession>> facultyIndex = new HashMap<>();

    /** batchDaySlot -> sessions */
    @Getter
    private final Map<String, List<ScheduledSession>> batchIndex = new HashMap<>();

    /** All sessions in the draft */
    @Getter
    private final List<ScheduledSession> allSessions = new ArrayList<>();

    private static final String KEY_DELIMITER = ":";

    /**
     * Builds the key for room index lookup.
     */
    public static String roomKey(Long roomId, String dayOfWeek, Long slotDefinitionId) {
        return "R" + KEY_DELIMITER + roomId + KEY_DELIMITER + dayOfWeek + KEY_DELIMITER + slotDefinitionId;
    }

    /**
     * Builds the key for faculty index lookup.
     */
    public static String facultyKey(Long facultyId, String dayOfWeek, Long slotDefinitionId) {
        return "F" + KEY_DELIMITER + facultyId + KEY_DELIMITER + dayOfWeek + KEY_DELIMITER + slotDefinitionId;
    }

    /**
     * Builds the key for batch index lookup.
     */
    public static String batchKey(Long batchId, String dayOfWeek, Long slotDefinitionId) {
        return "B" + KEY_DELIMITER + batchId + KEY_DELIMITER + dayOfWeek + KEY_DELIMITER + slotDefinitionId;
    }

    /**
     * Clears all indices.
     */
    public void clear() {
        roomIndex.clear();
        facultyIndex.clear();
        batchIndex.clear();
        allSessions.clear();
    }

    /**
     * Adds a session to all indices.
     */
    public void addSession(ScheduledSession session) {
        if (session == null || session.getRoomId() == null || session.getDayOfWeek() == null || session.getSlotDefinitionId() == null) {
            return;
        }

        allSessions.add(session);

        // Room index
        String roomKey = roomKey(session.getRoomId(), session.getDayOfWeek(), session.getSlotDefinitionId());
        roomIndex.computeIfAbsent(roomKey, k -> new ArrayList<>()).add(session);

        // Faculty index
        if (session.getFacultyId() != null) {
            String facultyKey = facultyKey(session.getFacultyId(), session.getDayOfWeek(), session.getSlotDefinitionId());
            facultyIndex.computeIfAbsent(facultyKey, k -> new ArrayList<>()).add(session);
        }

        // Batch index
        if (session.getBatchId() != null) {
            String batchKey = batchKey(session.getBatchId(), session.getDayOfWeek(), session.getSlotDefinitionId());
            batchIndex.computeIfAbsent(batchKey, k -> new ArrayList<>()).add(session);
        }
    }

    /**
     * Removes a session from all indices.
     */
    public void removeSession(ScheduledSession session) {
        if (session == null) {
            return;
        }

        allSessions.removeIf(s -> s.getId().equals(session.getId()));

        // Room index
        if (session.getRoomId() != null && session.getDayOfWeek() != null && session.getSlotDefinitionId() != null) {
            String roomKey = roomKey(session.getRoomId(), session.getDayOfWeek(), session.getSlotDefinitionId());
            var sessions = roomIndex.get(roomKey);
            if (sessions != null) {
                sessions.removeIf(s -> s.getId().equals(session.getId()));
                if (sessions.isEmpty()) {
                    roomIndex.remove(roomKey);
                }
            }
        }

        // Faculty index
        if (session.getFacultyId() != null && session.getDayOfWeek() != null && session.getSlotDefinitionId() != null) {
            String facultyKey = facultyKey(session.getFacultyId(), session.getDayOfWeek(), session.getSlotDefinitionId());
            var sessions = facultyIndex.get(facultyKey);
            if (sessions != null) {
                sessions.removeIf(s -> s.getId().equals(session.getId()));
                if (sessions.isEmpty()) {
                    facultyIndex.remove(facultyKey);
                }
            }
        }

        // Batch index
        if (session.getBatchId() != null && session.getDayOfWeek() != null && session.getSlotDefinitionId() != null) {
            String batchKey = batchKey(session.getBatchId(), session.getDayOfWeek(), session.getSlotDefinitionId());
            var sessions = batchIndex.get(batchKey);
            if (sessions != null) {
                sessions.removeIf(s -> s.getId().equals(session.getId()));
                if (sessions.isEmpty()) {
                    batchIndex.remove(batchKey);
                }
            }
        }
    }

    /**
     * Gets all sessions in a room at a given day+slot.
     */
    public List<ScheduledSession> getSessionsByRoom(Long roomId, String dayOfWeek, Long slotDefinitionId) {
        return roomIndex.getOrDefault(roomKey(roomId, dayOfWeek, slotDefinitionId), Collections.emptyList());
    }

    /**
     * Gets all sessions for a faculty at a given day+slot.
     */
    public List<ScheduledSession> getSessionsByFaculty(Long facultyId, String dayOfWeek, Long slotDefinitionId) {
        return facultyIndex.getOrDefault(facultyKey(facultyId, dayOfWeek, slotDefinitionId), Collections.emptyList());
    }

    /**
     * Gets all sessions for a batch at a given day+slot.
     */
    public List<ScheduledSession> getSessionsByBatch(Long batchId, String dayOfWeek, Long slotDefinitionId) {
        return batchIndex.getOrDefault(batchKey(batchId, dayOfWeek, slotDefinitionId), Collections.emptyList());
    }

    /**
     * Gets all sessions for a faculty on a given day.
     */
    public List<ScheduledSession> getSessionsByFacultyAndDay(Long facultyId, String dayOfWeek) {
        List<ScheduledSession> result = new ArrayList<>();
        String prefix = "F" + KEY_DELIMITER + facultyId + KEY_DELIMITER + dayOfWeek + KEY_DELIMITER;
        for (var entry : facultyIndex.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.addAll(entry.getValue());
            }
        }
        return result;
    }

    /**
     * Gets all sessions for a faculty in the entire draft.
     */
    public List<ScheduledSession> getSessionsByFaculty(Long facultyId) {
        List<ScheduledSession> result = new ArrayList<>();
        String prefix = "F" + KEY_DELIMITER + facultyId + KEY_DELIMITER;
        for (var entry : facultyIndex.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.addAll(entry.getValue());
            }
        }
        return result;
    }

    /**
     * Checks if a room+day+slot combination is occupied.
     */
    public boolean isRoomOccupied(Long roomId, String dayOfWeek, Long slotDefinitionId, Long excludeSessionId) {
        var sessions = getSessionsByRoom(roomId, dayOfWeek, slotDefinitionId);
        return sessions.stream().anyMatch(s -> !s.getId().equals(excludeSessionId));
    }

    /**
     * Checks if a faculty+day+slot combination is occupied.
     */
    public boolean isFacultyOccupied(Long facultyId, String dayOfWeek, Long slotDefinitionId, Long excludeSessionId) {
        var sessions = getSessionsByFaculty(facultyId, dayOfWeek, slotDefinitionId);
        return sessions.stream().anyMatch(s -> !s.getId().equals(excludeSessionId));
    }

    /**
     * Checks if a batch+day+slot combination is occupied.
     */
    public boolean isBatchOccupied(Long batchId, String dayOfWeek, Long slotDefinitionId, Long excludeSessionId) {
        var sessions = getSessionsByBatch(batchId, dayOfWeek, slotDefinitionId);
        return sessions.stream().anyMatch(s -> !s.getId().equals(excludeSessionId));
    }
}
