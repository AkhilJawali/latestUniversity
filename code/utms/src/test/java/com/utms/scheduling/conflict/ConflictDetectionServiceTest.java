package com.utms.scheduling.conflict;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.entity.TimetableDraft;
import com.utms.scheduling.engine.enums.DraftStatus;
import com.utms.scheduling.engine.repository.ScheduledSessionRepository;
import com.utms.scheduling.engine.repository.TimetableDraftRepository;

import com.utms.common.exception.EntityNotFoundException;

/**
 * Unit tests for ConflictDetectionService.
 * 
 * AC8: Malformed input returns 400-style error (via IllegalArgumentException).
 * AC9: Full-draft check returns all conflicts.
 */
@ExtendWith(MockitoExtension.class)
class ConflictDetectionServiceTest {

    @Mock
    private DraftOccupancyLoader occupancyLoader;

    @Mock
    private PlacementRuleChecker ruleChecker;

    @Mock
    private ScheduledSessionRepository sessionRepository;

    @Mock
    private TimetableDraftRepository draftRepository;

    @InjectMocks
    private ConflictDetectionService detectionService;

    @Test
    @DisplayName("Should check placement and return conflicts")
    void testCheckPlacement() {
        // Setup
        TimetableDraft draft = new TimetableDraft();
        draft.setId(1L);
        
        ProposedPlacementRequest request = ProposedPlacementRequest.builder()
                .draftId(1L)
                .sessionId(10L)
                .roomId(100L)
                .facultyId(200L)
                .batchId(300L)
                .dayOfWeek("MONDAY")
                .slotDefinitionId(5L)
                .build();
        
        DraftOccupancyIndex index = new DraftOccupancyIndex();
        
        when(draftRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draft));
        when(occupancyLoader.loadForDraft(1L)).thenReturn(index);
        when(sessionRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.empty());
        when(ruleChecker.checkPlacement(request, null, index)).thenReturn(List.of());
        
        // Execute
        List<ConflictDto> conflicts = detectionService.checkPlacement(request);
        
        // Verify
        assertTrue(conflicts.isEmpty());
        verify(occupancyLoader).loadForDraft(1L);
    }

    // AC8: Malformed input returns error

    @Test
    @DisplayName("AC8: Should throw when draft not found")
    void testCheckPlacement_draftNotFound() {
        ProposedPlacementRequest request = ProposedPlacementRequest.builder()
                .draftId(999L)
                .build();
        
        when(draftRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());
        
        assertThrows(EntityNotFoundException.class, () -> detectionService.checkPlacement(request));
    }

    // AC9: Full-draft check

    @Test
    @DisplayName("AC9: Should check entire draft for conflicts")
    void testCheckDraft() {
        // Setup
        TimetableDraft draft = new TimetableDraft();
        draft.setId(1L);
        
        ScheduledSession session1 = new ScheduledSession();
        session1.setId(1L);
        session1.setDayOfWeek("MONDAY");
        session1.setRoomId(10L);
        session1.setFacultyId(20L);
        session1.setBatchId(30L);
        session1.setSlotDefinitionId(5L);
        
        DraftOccupancyIndex index = new DraftOccupancyIndex();
        index.addSession(session1);
        
        when(draftRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draft));
        when(occupancyLoader.loadForDraft(1L)).thenReturn(index);
        when(ruleChecker.checkPlacement(any(), any(), any())).thenReturn(List.of());
        
        // Execute
        List<ConflictDto> conflicts = detectionService.checkDraft(1L);
        
        // Verify
        assertNotNull(conflicts);
        verify(occupancyLoader).loadForDraft(1L);
    }

    @Test
    @DisplayName("AC9: Should throw when draft not found for full check")
    void testCheckDraft_draftNotFound() {
        when(draftRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());
        
        assertThrows(EntityNotFoundException.class, () -> detectionService.checkDraft(999L));
    }

    @Test
    @DisplayName("Should check cross-draft conflicts")
    void testCheckCrossDraftConflicts() {
        // Setup - current draft has a session
        ScheduledSession session = new ScheduledSession();
        session.setId(1L);
        session.setDayOfWeek("MONDAY");
        session.setRoomId(10L);
        session.setFacultyId(20L);
        session.setSlotDefinitionId(5L);
        
        DraftOccupancyIndex currentIndex = new DraftOccupancyIndex();
        currentIndex.addSession(session);
        
        // Other draft is empty
        DraftOccupancyIndex otherIndex = new DraftOccupancyIndex();
        
        when(occupancyLoader.loadForDraft(100L)).thenReturn(currentIndex);
        when(occupancyLoader.loadForDrafts(List.of(200L))).thenReturn(otherIndex);
        
        // Execute
        List<ConflictDto> conflicts = detectionService.checkCrossDraftConflicts(100L, List.of(200L));
        
        // Verify
        assertNotNull(conflicts);
        assertTrue(conflicts.isEmpty()); // No cross-draft conflicts since other is empty
    }

    @Test
    @DisplayName("Should deduplicate conflicts")
    void testDeduplicateConflicts() {
        // Create duplicate conflicts (same type and conflicting session)
        ConflictDto c1 = ConflictDto.from(
                ConflictType.ROOM_DOUBLE_BOOKING,
                1L, "MONDAY", 1L, 100L, null, null, null,
                List.of(2L), "Conflict 1");
        
        ConflictDto c2 = ConflictDto.from(
                ConflictType.ROOM_DOUBLE_BOOKING,
                1L, "MONDAY", 1L, 100L, null, null, null,
                List.of(2L), "Conflict 2 (duplicate)");
        
        // Setup draft and index
        TimetableDraft draft = new TimetableDraft();
        draft.setId(1L);
        
        ScheduledSession session = new ScheduledSession();
        session.setId(1L);
        session.setRoomId(100L);
        session.setDayOfWeek("MONDAY");
        session.setSlotDefinitionId(1L);
        
        DraftOccupancyIndex index = new DraftOccupancyIndex();
        index.addSession(session);
        
        when(draftRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draft));
        when(occupancyLoader.loadForDraft(1L)).thenReturn(index);
        when(ruleChecker.checkPlacement(any(), any(), any())).thenReturn(List.of(c1, c2));
        
        List<ConflictDto> result = detectionService.checkDraft(1L);
        
        // Duplicates should be removed
        assertEquals(1, result.size());
    }
}
