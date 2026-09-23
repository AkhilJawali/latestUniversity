package com.utms.scheduling.conflict;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.enums.RecurrenceType;
import com.utms.scheduling.engine.enums.WeekGroup;

/**
 * Unit tests for DraftOccupancyIndex.
 * 
 * AC6: No conflict for valid placement (index returns correct occupancy).
 */
class DraftOccupancyIndexTest {

    private DraftOccupancyIndex index;

    @BeforeEach
    void setUp() {
        index = new DraftOccupancyIndex();
    }

    @Test
    @DisplayName("Should add session to room index")
    void testAddSession_roomIndex() {
        ScheduledSession session = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        
        index.addSession(session);
        
        List<ScheduledSession> sessions = index.getSessionsByRoom(100L, "MONDAY", 1L);
        assertEquals(1, sessions.size());
        assertEquals(1L, sessions.get(0).getId());
    }

    @Test
    @DisplayName("Should add session to faculty index")
    void testAddSession_facultyIndex() {
        ScheduledSession session = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        
        index.addSession(session);
        
        List<ScheduledSession> sessions = index.getSessionsByFaculty(200L, "MONDAY", 1L);
        assertEquals(1, sessions.size());
    }

    @Test
    @DisplayName("Should add session to batch index")
    void testAddSession_batchIndex() {
        ScheduledSession session = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        
        index.addSession(session);
        
        List<ScheduledSession> sessions = index.getSessionsByBatch(300L, "MONDAY", 1L);
        assertEquals(1, sessions.size());
    }

    @Test
    @DisplayName("Should detect room occupancy")
    void testIsRoomOccupied() {
        ScheduledSession session = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        index.addSession(session);
        
        assertTrue(index.isRoomOccupied(100L, "MONDAY", 1L, null));
        assertFalse(index.isRoomOccupied(100L, "TUESDAY", 1L, null));
    }

    @Test
    @DisplayName("Should exclude session from occupancy check")
    void testIsRoomOccupied_excludeSelf() {
        ScheduledSession session = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        index.addSession(session);
        
        assertFalse(index.isRoomOccupied(100L, "MONDAY", 1L, 1L));
    }

    @Test
    @DisplayName("Should detect faculty occupancy")
    void testIsFacultyOccupied() {
        ScheduledSession session = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        index.addSession(session);
        
        assertTrue(index.isFacultyOccupied(200L, "MONDAY", 1L, null));
        assertFalse(index.isFacultyOccupied(200L, "TUESDAY", 1L, null));
    }

    @Test
    @DisplayName("Should detect batch occupancy")
    void testIsBatchOccupied() {
        ScheduledSession session = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        index.addSession(session);
        
        assertTrue(index.isBatchOccupied(300L, "MONDAY", 1L, null));
        assertFalse(index.isBatchOccupied(300L, "TUESDAY", 1L, null));
    }

    @Test
    @DisplayName("Should remove session from all indices")
    void testRemoveSession() {
        ScheduledSession session = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        index.addSession(session);
        
        index.removeSession(session);
        
        assertTrue(index.getSessionsByRoom(100L, "MONDAY", 1L).isEmpty());
        assertTrue(index.getSessionsByFaculty(200L, "MONDAY", 1L).isEmpty());
        assertTrue(index.getSessionsByBatch(300L, "MONDAY", 1L).isEmpty());
    }

    @Test
    @DisplayName("Should clear all indices")
    void testClear() {
        index.addSession(createSession(1L, 100L, 200L, 300L, "MONDAY", 1L));
        index.addSession(createSession(2L, 101L, 201L, 301L, "TUESDAY", 2L));
        
        index.clear();
        
        assertTrue(index.getAllSessions().isEmpty());
    }

    @Test
    @DisplayName("Should get sessions by faculty and day")
    void testGetSessionsByFacultyAndDay() {
        index.addSession(createSession(1L, 100L, 200L, 300L, "MONDAY", 1L));
        index.addSession(createSession(2L, 100L, 200L, 301L, "MONDAY", 2L));
        index.addSession(createSession(3L, 100L, 200L, 302L, "TUESDAY", 1L));
        
        List<ScheduledSession> mondaySessions = index.getSessionsByFacultyAndDay(200L, "MONDAY");
        
        assertEquals(2, mondaySessions.size());
    }

    @Test
    @DisplayName("Should handle null session gracefully")
    void testAddSession_null() {
        index.addSession(null);
        
        assertTrue(index.getAllSessions().isEmpty());
    }

    // Helper methods

    private ScheduledSession createSession(Long id, Long roomId, Long facultyId, Long batchId, String day, Long slotId) {
        ScheduledSession session = new ScheduledSession();
        session.setId(id);
        session.setRoomId(roomId);
        session.setFacultyId(facultyId);
        session.setBatchId(batchId);
        session.setDayOfWeek(day);
        session.setSlotDefinitionId(slotId);
        session.setRecurrenceType(RecurrenceType.WEEKLY);
        return session;
    }
}
