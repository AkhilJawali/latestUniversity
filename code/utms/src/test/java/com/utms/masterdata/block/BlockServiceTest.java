package com.utms.masterdata.block;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.asset.SchedulableAssetRepository;
import com.utms.masterdata.room.Room;
import com.utms.masterdata.room.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock
    private ResourceBlockRepository blockRepository;

    @Mock
    private BlockApprovalActionRepository approvalActionRepository;

    @Mock
    private SoftBlockOverrideRepository overrideRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private SchedulableAssetRepository assetRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BlockService blockService;

    // --- raiseBlock ---

    @Test
    void raiseBlock_noImpact_setsActiveImmediately() {
        CreateBlockRequest request = CreateBlockRequest.builder()
                .resourceType("ROOM")
                .resourceId(1L)
                .blockType("HARD")
                .startDate(LocalDate.of(2025, 3, 1))
                .endDate(LocalDate.of(2025, 3, 5))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .reasonCode("MAINTENANCE")
                .reasonNote("Scheduled painting")
                .build();

        Room room = new Room();
        room.setId(1L);
        when(roomRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(room));

        ResourceBlock saved = createBlock(1L, "ROOM", 1L, "HARD", "ACTIVE");
        saved.setActivatedAt(LocalDateTime.now());
        when(blockRepository.save(any(ResourceBlock.class))).thenReturn(saved);

        ResourceBlockDto result = blockService.raiseBlock(request);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        verify(eventPublisher).publishEvent(any(ResourceBlockActivatedEvent.class));
        verify(blockRepository).save(any(ResourceBlock.class));
    }

    @Test
    void raiseBlock_invalidResource_throwsEntityNotFound() {
        CreateBlockRequest request = CreateBlockRequest.builder()
                .resourceType("ROOM")
                .resourceId(99L)
                .blockType("SOFT")
                .startDate(LocalDate.of(2025, 3, 1))
                .endDate(LocalDate.of(2025, 3, 5))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .reasonCode("EVENT")
                .build();

        when(roomRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> blockService.raiseBlock(request));
        verify(blockRepository, never()).save(any());
    }

    // --- approveBlock ---

    @Test
    void approveBlock_pendingBlock_activatesAndEmitsEvent() {
        ResourceBlock block = createBlock(1L, "ROOM", 1L, "HARD", "PENDING_APPROVAL");
        when(blockRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(block));
        when(blockRepository.save(any(ResourceBlock.class))).thenReturn(block);

        BlockApprovalRequest request = BlockApprovalRequest.builder()
                .comments("Approved for maintenance window")
                .build();

        ResourceBlockDto result = blockService.approveBlock(1L, request);

        assertEquals("ACTIVE", result.getStatus());
        assertNotNull(block.getActivatedAt());

        ArgumentCaptor<ResourceBlockActivatedEvent> captor =
                ArgumentCaptor.forClass(ResourceBlockActivatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(1L, captor.getValue().blockId());
        assertEquals("ROOM", captor.getValue().resourceType());

        verify(approvalActionRepository).save(any(BlockApprovalAction.class));
    }

    @Test
    void approveBlock_notPending_throwsBusinessRuleViolation() {
        ResourceBlock block = createBlock(1L, "ROOM", 1L, "HARD", "ACTIVE");
        when(blockRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(block));

        assertThrows(BusinessRuleViolationException.class,
                () -> blockService.approveBlock(1L, null));
        verify(blockRepository, never()).save(any());
    }

    // --- rejectBlock ---

    @Test
    void rejectBlock_pendingBlock_setsRejected() {
        ResourceBlock block = createBlock(1L, "ASSET", 2L, "HARD", "PENDING_APPROVAL");
        when(blockRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(block));
        when(blockRepository.save(any(ResourceBlock.class))).thenReturn(block);

        BlockRejectRequest request = BlockRejectRequest.builder()
                .reason("Not justified")
                .build();

        ResourceBlockDto result = blockService.rejectBlock(1L, request);

        assertEquals("REJECTED", result.getStatus());
        verify(approvalActionRepository).save(any(BlockApprovalAction.class));
        verify(eventPublisher, never()).publishEvent(any(ResourceBlockActivatedEvent.class));
    }

    // --- withdrawBlock ---

    @Test
    void withdrawBlock_pendingBlock_setsWithdrawn() {
        ResourceBlock block = createBlock(1L, "ROOM", 1L, "SOFT", "PENDING_APPROVAL");
        when(blockRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(block));
        when(blockRepository.save(any(ResourceBlock.class))).thenReturn(block);

        ResourceBlockDto result = blockService.withdrawBlock(1L);

        assertEquals("WITHDRAWN", result.getStatus());
        verify(approvalActionRepository).save(any(BlockApprovalAction.class));
    }

    // --- releaseBlock ---

    @Test
    void releaseBlock_activeBlock_setsReleased() {
        ResourceBlock block = createBlock(1L, "ROOM", 1L, "HARD", "ACTIVE");
        when(blockRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(block));
        when(blockRepository.save(any(ResourceBlock.class))).thenReturn(block);

        ResourceBlockDto result = blockService.releaseBlock(1L);

        assertEquals("RELEASED", result.getStatus());
        assertNotNull(block.getReleasedAt());
    }

    @Test
    void releaseBlock_notActive_throwsBusinessRuleViolation() {
        ResourceBlock block = createBlock(1L, "ROOM", 1L, "HARD", "PENDING_APPROVAL");
        when(blockRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(block));

        assertThrows(BusinessRuleViolationException.class,
                () -> blockService.releaseBlock(1L));
        verify(blockRepository, never()).save(any());
    }

    // --- recordSoftBlockOverride (KD-33) ---

    @Test
    void recordSoftBlockOverride_activeSoftBlock_savesOverride() {
        ResourceBlock block = createBlock(1L, "ROOM", 1L, "SOFT", "ACTIVE");
        when(blockRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(block));

        blockService.recordSoftBlockOverride(1L, "Critical lecture must proceed", 42L);

        ArgumentCaptor<SoftBlockOverride> captor = ArgumentCaptor.forClass(SoftBlockOverride.class);
        verify(overrideRepository).save(captor.capture());

        SoftBlockOverride saved = captor.getValue();
        assertEquals(1L, saved.getBlock().getId());
        assertEquals(42L, saved.getSessionId());
        assertEquals("Critical lecture must proceed", saved.getJustification());
    }

    @Test
    void recordSoftBlockOverride_hardBlock_throwsBusinessRuleViolation() {
        ResourceBlock block = createBlock(1L, "ROOM", 1L, "HARD", "ACTIVE");
        when(blockRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(block));

        assertThrows(BusinessRuleViolationException.class,
                () -> blockService.recordSoftBlockOverride(1L, "Some reason", null));
        verify(overrideRepository, never()).save(any());
    }

    // --- expirePendingBlocks ---

    @Test
    void expirePendingBlocks_expiredBlocks_setsExpiredStatus() {
        ResourceBlock block1 = createBlock(1L, "ROOM", 1L, "HARD", "PENDING_APPROVAL");
        block1.setExpiresAt(LocalDateTime.now().minusHours(1));
        ResourceBlock block2 = createBlock(2L, "ASSET", 3L, "SOFT", "PENDING_APPROVAL");
        block2.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(blockRepository.findByStatusAndExpiresAtBeforeAndDeletedAtIsNull(
                eq("PENDING_APPROVAL"), any(LocalDateTime.class)))
                .thenReturn(List.of(block1, block2));

        blockService.expirePendingBlocks();

        assertEquals("EXPIRED", block1.getStatus());
        assertEquals("EXPIRED", block2.getStatus());
        verify(blockRepository, times(2)).save(any(ResourceBlock.class));
    }

    // --- Helper ---

    private ResourceBlock createBlock(Long id, String resourceType, Long resourceId,
                                       String blockType, String status) {
        ResourceBlock block = new ResourceBlock();
        block.setId(id);
        block.setResourceType(resourceType);
        block.setResourceId(resourceId);
        block.setBlockType(blockType);
        block.setStatus(status);
        block.setStartDate(LocalDate.of(2025, 3, 1));
        block.setEndDate(LocalDate.of(2025, 3, 5));
        block.setStartTime(LocalTime.of(9, 0));
        block.setEndTime(LocalTime.of(17, 0));
        block.setReasonCode("MAINTENANCE");
        block.setRaisedBy("system");
        block.setRaisedAt(LocalDateTime.now());
        block.setIsActive(true);
        return block;
    }
}
