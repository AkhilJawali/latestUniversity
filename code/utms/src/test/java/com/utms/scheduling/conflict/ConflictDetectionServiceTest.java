package com.utms.scheduling.conflict;

import com.utms.common.exception.EntityNotFoundException;
import com.utms.scheduling.engine.entity.TimetableDraft;
import com.utms.scheduling.engine.enums.RecurrenceType;
import com.utms.scheduling.engine.repository.TimetableDraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConflictDetectionService} — A4-16 acceptance criteria AC6, AC8, AC9
 * (plus service-level conflict propagation). Controller-level HTTP status mapping
 * (400/404 envelope, malformed-body validation) is an integration-test concern.
 */
class ConflictDetectionServiceTest {

    private TimetableDraftRepository draftRepository;
    private DraftOccupancyLoader occupancyLoader;
    private PlacementRuleChecker ruleChecker;
    private ConflictDetectionService service;

    @BeforeEach
    void setUp() {
        draftRepository = mock(TimetableDraftRepository.class);
        occupancyLoader = mock(DraftOccupancyLoader.class);
        ruleChecker = mock(PlacementRuleChecker.class);
        service = new ConflictDetectionService(draftRepository, occupancyLoader, ruleChecker);
    }

    private ProposedPlacementRequest placement() {
        return ProposedPlacementRequest.builder()
                .facultyId(1L).roomId(5L).batchId(7L)
                .dayOfWeek("MONDAY").slotDefinitionId(3L).durationMinutes(60)
                .build();
    }

    // AC8: nonexistent / deleted draft -> EntityNotFoundException, no occupancy load.
    @Test
    void checkPlacement_draftNotFound_throwsAndDoesNotLoadOccupancy() {
        when(draftRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkPlacement(99L, placement()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("TimetableDraft");

        verify(occupancyLoader, never()).load(anyLong());
    }

    // AC6: valid draft, checker finds nothing -> empty list.
    @Test
    void checkPlacement_noConflicts_returnsEmpty() {
        when(draftRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(new TimetableDraft()));
        when(occupancyLoader.load(1L)).thenReturn(new DraftOccupancyIndex());
        when(ruleChecker.check(any(), any())).thenReturn(List.of());

        List<ConflictDto> conflicts = service.checkPlacement(1L, placement());
        assertThat(conflicts).isEmpty();
    }

    // Service plumbing: checker conflicts are propagated to the caller.
    @Test
    void checkPlacement_checkerReturnsConflict_isPropagated() {
        when(draftRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(new TimetableDraft()));
        when(occupancyLoader.load(1L)).thenReturn(new DraftOccupancyIndex());
        when(ruleChecker.check(any(), any())).thenReturn(List.of(
                ConflictDto.builder().type(ConflictType.FACULTY_DOUBLE_BOOKING).build()));

        List<ConflictDto> conflicts = service.checkPlacement(1L, placement());
        assertThat(conflicts).extracting(ConflictDto::getType).contains(ConflictType.FACULTY_DOUBLE_BOOKING);
    }

    // AC9: full-draft check evaluates every placed session and aggregates conflicts.
    @Test
    void checkDraft_aggregatesConflictsAcrossPlacedSessions() {
        when(draftRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(new TimetableDraft()));

        DraftOccupancyIndex index = new DraftOccupancyIndex();
        index.add(new DraftOccupancyIndex.Occupant(10L, 1L, 5L, 7L, null, "MONDAY", 3L,
                1.0, RecurrenceType.WEEKLY, null));
        index.add(new DraftOccupancyIndex.Occupant(11L, 2L, 6L, 8L, null, "TUESDAY", 4L,
                1.0, RecurrenceType.WEEKLY, null));
        when(occupancyLoader.load(1L)).thenReturn(index);
        when(ruleChecker.check(any(), any())).thenReturn(List.of(
                ConflictDto.builder().type(ConflictType.BATCH_CLASH).build()));

        List<ConflictDto> conflicts = service.checkDraft(1L);

        // Two placed sessions -> checker invoked twice -> two aggregated conflicts.
        assertThat(conflicts).hasSize(2);
        verify(ruleChecker, times(2)).check(any(), any());
    }

    // AC8 (full-draft variant): nonexistent draft on full-draft check also throws.
    @Test
    void checkDraft_draftNotFound_throws() {
        when(draftRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.checkDraft(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(occupancyLoader, never()).load(anyLong());
    }
}
