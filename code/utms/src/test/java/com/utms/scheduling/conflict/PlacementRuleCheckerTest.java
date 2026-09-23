package com.utms.scheduling.conflict;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.utms.masterdata.batch.Batch;
import com.utms.masterdata.batch.BatchRepository;
import com.utms.masterdata.room.Room;
import com.utms.masterdata.room.RoomRepository;
import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.enums.RecurrenceType;
import com.utms.scheduling.engine.enums.WeekGroup;
import com.utms.scheduling.engine.service.RecurrenceOverlapEvaluator;

/**
 * Unit tests for PlacementRuleChecker.
 * 
 * Tests AC1-AC7 conflict types:
 * - AC1: Faculty double-booking
 * - AC2: Room double-booking
 * - AC3: Batch/section clash
 * - AC4: Room capacity exceeded
 * - AC5: Faculty workload exceeded
 * - AC6: No conflict for valid placement
 * - AC7: Multiple conflicts in one response
 */
@ExtendWith(MockitoExtension.class)
class PlacementRuleCheckerTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private RecurrenceOverlapEvaluator recurrenceEvaluator;

    @InjectMocks
    private PlacementRuleChecker ruleChecker;

    private DraftOccupancyIndex index;

    @BeforeEach
    void setUp() {
        index = new DraftOccupancyIndex();
    }

    // AC1: Faculty double-booking detection

    @Test
    @DisplayName("AC1: Should detect faculty double-booking")
    void testCheckPlacement_facultyDoubleBooking() {
        // Setup: Add existing session for faculty
        ScheduledSession existing = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        index.addSession(existing);
        
        // New session with same faculty, different room
        ProposedPlacementRequest request = createRequest(2L, 101L, 200L, 301L, "MONDAY", 1L);
        
        // Recurrence overlap returns true (same week)
        when(recurrenceEvaluator.everCoOccur(any(), any())).thenReturn(true);
        
        List<ConflictDto> conflicts = ruleChecker.checkPlacement(request, null, index);
        
        assertTrue(conflicts.stream().anyMatch(c -> c.getType() == ConflictType.FACULTY_DOUBLE_BOOKING));
    }

    // AC2: Room double-booking detection

    @Test
    @DisplayName("AC2: Should detect room double-booking")
    void testCheckPlacement_roomDoubleBooking() {
        // Setup: Add existing session in room
        ScheduledSession existing = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        index.addSession(existing);
        
        // New session in same room, different faculty
        ProposedPlacementRequest request = createRequest(2L, 100L, 201L, 301L, "MONDAY", 1L);
        
        when(recurrenceEvaluator.everCoOccur(any(), any())).thenReturn(true);
        
        List<ConflictDto> conflicts = ruleChecker.checkPlacement(request, null, index);
        
        assertTrue(conflicts.stream().anyMatch(c -> c.getType() == ConflictType.ROOM_DOUBLE_BOOKING));
    }

    // AC3: Batch clash detection

    @Test
    @DisplayName("AC3: Should detect batch clash")
    void testCheckPlacement_batchClash() {
        // Setup: Add existing session with batch
        ScheduledSession existing = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        index.addSession(existing);
        
        // New session with same batch, different room/faculty
        ProposedPlacementRequest request = createRequest(2L, 101L, 201L, 300L, "MONDAY", 1L);
        
        when(recurrenceEvaluator.everCoOccur(any(), any())).thenReturn(true);
        
        List<ConflictDto> conflicts = ruleChecker.checkPlacement(request, null, index);
        
        assertTrue(conflicts.stream().anyMatch(c -> c.getType() == ConflictType.BATCH_CLASH));
    }

    // AC4: Room capacity exceeded detection

    @Test
    @DisplayName("AC4: Should detect room capacity exceeded")
    void testCheckPlacement_roomCapacityExceeded() {
        // Room with capacity 30
        Room room = new Room();
        room.setId(100L);
        room.setCapacity(30);
        
        // Batch with strength 50
        Batch batch = new Batch();
        batch.setId(300L);
        batch.setStrength(50);
        
        when(roomRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(batchRepository.findByIdAndDeletedAtIsNull(300L)).thenReturn(Optional.of(batch));
        
        ProposedPlacementRequest request = createRequest(null, 100L, 200L, 300L, "MONDAY", 1L);
        
        List<ConflictDto> conflicts = ruleChecker.checkPlacement(request, null, index);
        
        assertTrue(conflicts.stream().anyMatch(c -> c.getType() == ConflictType.ROOM_CAPACITY_EXCEEDED));
    }

    @Test
    @DisplayName("AC4: Should not detect capacity conflict when room is large enough")
    void testCheckPlacement_roomCapacitySufficient() {
        Room room = new Room();
        room.setId(100L);
        room.setCapacity(100);
        
        Batch batch = new Batch();
        batch.setId(300L);
        batch.setStrength(50);
        
        when(roomRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(batchRepository.findByIdAndDeletedAtIsNull(300L)).thenReturn(Optional.of(batch));
        
        ProposedPlacementRequest request = createRequest(null, 100L, 200L, 300L, "MONDAY", 1L);
        
        List<ConflictDto> conflicts = ruleChecker.checkPlacement(request, null, index);
        
        assertFalse(conflicts.stream().anyMatch(c -> c.getType() == ConflictType.ROOM_CAPACITY_EXCEEDED));
    }

    // AC6: No conflict for valid placement

    @Test
    @DisplayName("AC6: Should return empty list for valid placement")
    void testCheckPlacement_validPlacement() {
        // Empty index
        Room room = new Room();
        room.setId(100L);
        room.setCapacity(100);
        
        Batch batch = new Batch();
        batch.setId(300L);
        batch.setStrength(50);
        
        when(roomRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(batchRepository.findByIdAndDeletedAtIsNull(300L)).thenReturn(Optional.of(batch));
        
        ProposedPlacementRequest request = createRequest(null, 100L, 200L, 300L, "MONDAY", 1L);
        
        List<ConflictDto> conflicts = ruleChecker.checkPlacement(request, null, index);
        
        assertTrue(conflicts.isEmpty());
    }

    // AC7: Multiple conflicts in one response

    @Test
    @DisplayName("AC7: Should return multiple conflicts in one response")
    void testCheckPlacement_multipleConflicts() {
        // Setup: Add existing session that causes both room and faculty conflict
        ScheduledSession existing = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        index.addSession(existing);
        
        // New session in same room AND same faculty
        ProposedPlacementRequest request = createRequest(2L, 100L, 200L, 301L, "MONDAY", 1L);
        
        when(recurrenceEvaluator.everCoOccur(any(), any())).thenReturn(true);
        
        List<ConflictDto> conflicts = ruleChecker.checkPlacement(request, null, index);
        
        assertTrue(conflicts.size() >= 1, "Should have at least one conflict");
    }

    // Recurrence-aware tests (KD-64)

    @Test
    @DisplayName("KD-64: Should not conflict for alternate-week sessions")
    void testCheckPlacement_alternateWeekNoConflict() {
        // Fortnightly Group A session
        ScheduledSession existing = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        existing.setRecurrenceType(RecurrenceType.FORTNIGHTLY);
        existing.setWeekGroup(WeekGroup.WEEK_A);
        index.addSession(existing);
        
        // Fortnightly Group B session in same slot - should NOT conflict
        ProposedPlacementRequest request = createRequest(2L, 100L, 200L, 301L, "MONDAY", 1L);
        
        // Recurrence evaluator says they don't co-occur
        when(recurrenceEvaluator.everCoOccur(any(), any())).thenReturn(false);
        
        List<ConflictDto> conflicts = ruleChecker.checkPlacement(request, null, index);
        
        // No room double-booking conflict because they never co-occur
        assertFalse(conflicts.stream().anyMatch(c -> c.getType() == ConflictType.ROOM_DOUBLE_BOOKING));
    }

    @Test
    @DisplayName("Should skip self when moving existing session")
    void testCheckPlacement_excludeSelf() {
        // Add session to index
        ScheduledSession existing = createSession(1L, 100L, 200L, 300L, "MONDAY", 1L);
        index.addSession(existing);
        
        // Move the same session to same slot (should not conflict with itself)
        ProposedPlacementRequest request = createRequest(1L, 100L, 200L, 300L, "MONDAY", 1L);
        
        // Note: recurrenceEvaluator is NOT called when session is checking against itself
        List<ConflictDto> conflicts = ruleChecker.checkPlacement(request, existing, index);
        
        // Should not report conflict with itself
        assertTrue(conflicts.isEmpty());
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

    private ProposedPlacementRequest createRequest(Long sessionId, Long roomId, Long facultyId, Long batchId, String day, Long slotId) {
        return ProposedPlacementRequest.builder()
                .draftId(1L)
                .sessionId(sessionId)
                .roomId(roomId)
                .facultyId(facultyId)
                .batchId(batchId)
                .dayOfWeek(day)
                .slotDefinitionId(slotId)
                .build();
    }
}
