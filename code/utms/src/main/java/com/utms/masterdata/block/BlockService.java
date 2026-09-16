package com.utms.masterdata.block;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.room.RoomRepository;
import com.utms.masterdata.asset.SchedulableAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockService {

    private static final String STATUS_PENDING = "PENDING_APPROVAL";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_RELEASED = "RELEASED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";

    private final ResourceBlockRepository blockRepository;
    private final BlockApprovalActionRepository approvalActionRepository;
    private final SoftBlockOverrideRepository overrideRepository;
    private final RoomRepository roomRepository;
    private final SchedulableAssetRepository assetRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ResourceBlockDto raiseBlock(CreateBlockRequest request) {
        validateResourceExists(request.getResourceType(), request.getResourceId());
        validateDateTimeRange(request);

        ResourceBlock block = new ResourceBlock();
        block.setResourceType(request.getResourceType());
        block.setResourceId(request.getResourceId());
        block.setBlockType(request.getBlockType());
        block.setStartDate(request.getStartDate());
        block.setEndDate(request.getEndDate());
        block.setStartTime(request.getStartTime());
        block.setEndTime(request.getEndTime());
        block.setReasonCode(request.getReasonCode());
        block.setReasonText(request.getReasonNote());
        block.setRecurrencePattern(request.getRecurrencePattern());
        block.setRaisedBy("system"); // TODO: extract from security context
        block.setRaisedAt(LocalDateTime.now());
        block.setIsActive(true);

        // Determine if approval is needed (KD-34: roles configurable)
        boolean impactsPublishedSessions = checkImpactOnPublishedSessions(
                request.getResourceType(), request.getResourceId());

        if (impactsPublishedSessions) {
            block.setStatus(STATUS_PENDING);
            block.setExpiresAt(LocalDateTime.now().plusDays(7)); // KD-35: pending blocks can expire
        } else {
            block.setStatus(STATUS_ACTIVE);
            block.setActivatedAt(LocalDateTime.now());
        }

        block = blockRepository.save(block);

        if (STATUS_ACTIVE.equals(block.getStatus())) {
            eventPublisher.publishEvent(new ResourceBlockActivatedEvent(
                    block.getId(), block.getResourceType(), block.getResourceId()));
            log.info("Block raised and activated immediately: id={}, resource={}:{}",
                    block.getId(), block.getResourceType(), block.getResourceId());
        } else {
            log.info("Block raised pending approval: id={}, resource={}:{}",
                    block.getId(), block.getResourceType(), block.getResourceId());
        }

        return toDto(block);
    }

    @Transactional(readOnly = true)
    public ResourceBlockDto findById(Long id) {
        ResourceBlock block = findActiveByIdOrThrow(id);
        return toDto(block);
    }

    @Transactional(readOnly = true)
    public Page<ResourceBlockDto> findAll(String resourceType, Long resourceId, String status, Pageable pageable) {
        Specification<ResourceBlock> spec = notDeleted();

        if (resourceType != null && !resourceType.isBlank()) {
            Specification<ResourceBlock> typeSpec = byResourceType(resourceType);
            spec = spec.and(typeSpec);
        }
        if (resourceId != null) {
            Specification<ResourceBlock> resIdSpec = byResourceId(resourceId);
            spec = spec.and(resIdSpec);
        }
        if (status != null && !status.isBlank()) {
            Specification<ResourceBlock> statusSpec = byStatus(status);
            spec = spec.and(statusSpec);
        }

        return blockRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Transactional
    public ResourceBlockDto approveBlock(Long id, BlockApprovalRequest request) {
        ResourceBlock block = findActiveByIdOrThrow(id);
        assertStatus(block, STATUS_PENDING, "approve");

        block.setStatus(STATUS_ACTIVE);
        block.setActivatedAt(LocalDateTime.now());
        block = blockRepository.save(block);

        BlockApprovalAction action = new BlockApprovalAction();
        action.setBlock(block);
        action.setAction("APPROVE");
        action.setActorId("system"); // TODO: extract from security context
        action.setComments(request != null ? request.getComments() : null);
        action.setIsActive(true);
        approvalActionRepository.save(action);

        // KD-32: emit event for A4-16 integration
        eventPublisher.publishEvent(new ResourceBlockActivatedEvent(
                block.getId(), block.getResourceType(), block.getResourceId()));

        log.info("Block approved and activated: id={}", id);
        return toDto(block);
    }

    @Transactional
    public ResourceBlockDto rejectBlock(Long id, BlockRejectRequest request) {
        ResourceBlock block = findActiveByIdOrThrow(id);
        assertStatus(block, STATUS_PENDING, "reject");

        block.setStatus(STATUS_REJECTED);
        block = blockRepository.save(block);

        BlockApprovalAction action = new BlockApprovalAction();
        action.setBlock(block);
        action.setAction("REJECT");
        action.setActorId("system"); // TODO: extract from security context
        action.setComments(request.getReason());
        action.setIsActive(true);
        approvalActionRepository.save(action);

        log.info("Block rejected: id={}, reason={}", id, request.getReason());
        return toDto(block);
    }

    @Transactional
    public ResourceBlockDto withdrawBlock(Long id) {
        ResourceBlock block = findActiveByIdOrThrow(id);
        assertStatus(block, STATUS_PENDING, "withdraw");

        block.setStatus(STATUS_WITHDRAWN);
        block = blockRepository.save(block);

        BlockApprovalAction action = new BlockApprovalAction();
        action.setBlock(block);
        action.setAction("WITHDRAW");
        action.setActorId("system"); // TODO: extract from security context
        action.setIsActive(true);
        approvalActionRepository.save(action);

        log.info("Block withdrawn: id={}", id);
        return toDto(block);
    }

    @Transactional
    public ResourceBlockDto releaseBlock(Long id) {
        ResourceBlock block = findActiveByIdOrThrow(id);
        assertStatus(block, STATUS_ACTIVE, "release");

        block.setStatus(STATUS_RELEASED);
        block.setReleasedAt(LocalDateTime.now());
        block = blockRepository.save(block);

        log.info("Block released: id={}", id);
        return toDto(block);
    }

    /**
     * KD-33: Explicit write path for soft-block override recording.
     * Records that a soft block was overridden with justification.
     */
    @Transactional
    public void recordSoftBlockOverride(Long blockId, String justification, Long sessionId) {
        ResourceBlock block = findActiveByIdOrThrow(blockId);

        if (!"SOFT".equals(block.getBlockType())) {
            throw new BusinessRuleViolationException(
                    "Only SOFT blocks can be overridden", List.of(
                    Map.<String, Object>of("field", "blockType", "value", block.getBlockType())));
        }
        if (!STATUS_ACTIVE.equals(block.getStatus())) {
            throw new BusinessRuleViolationException(
                    "Block must be ACTIVE to record override", List.of(
                    Map.<String, Object>of("field", "status", "value", block.getStatus())));
        }

        SoftBlockOverride override = new SoftBlockOverride();
        override.setBlock(block);
        override.setSessionId(sessionId);
        override.setJustification(justification);
        override.setOverriddenBy("system"); // TODO: extract from security context
        override.setOverriddenAt(LocalDateTime.now());
        override.setIsActive(true);
        overrideRepository.save(override);

        log.info("Soft-block override recorded: blockId={}, sessionId={}", blockId, sessionId);
    }

    /**
     * KD-35: Hourly scheduled job to expire pending blocks past their expiry timestamp.
     */
    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void expirePendingBlocks() {
        List<ResourceBlock> expired = blockRepository
                .findByStatusAndExpiresAtBeforeAndDeletedAtIsNull(STATUS_PENDING, LocalDateTime.now());

        for (ResourceBlock block : expired) {
            block.setStatus(STATUS_EXPIRED);
            blockRepository.save(block);
            log.info("Pending block expired: id={}", block.getId());
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} pending blocks", expired.size());
        }
    }

    // --- Private helpers ---

    private void validateResourceExists(String resourceType, Long resourceId) {
        boolean exists = switch (resourceType) {
            case "ROOM" -> roomRepository.findByIdAndDeletedAtIsNull(resourceId).isPresent();
            case "ASSET" -> assetRepository.findByIdAndDeletedAtIsNull(resourceId).isPresent();
            default -> false;
        };
        if (!exists) {
            throw new EntityNotFoundException(resourceType, resourceId);
        }
    }

    private void validateDateTimeRange(CreateBlockRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessRuleViolationException(
                    "Start date must be before or equal to end date", List.of(
                    Map.<String, Object>of("field", "startDate", "constraint", "startDate <= endDate")));
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessRuleViolationException(
                    "Start time must be before end time", List.of(
                    Map.<String, Object>of("field", "startTime", "constraint", "startTime < endTime")));
        }
    }

    private boolean checkImpactOnPublishedSessions(String resourceType, Long resourceId) {
        // TODO: Check if there are published sessions using this resource in the block window.
        // For now, returns false (no impact). Approval logic enhanced in integration phase.
        return false;
    }

    private void assertStatus(ResourceBlock block, String expectedStatus, String operation) {
        if (!expectedStatus.equals(block.getStatus())) {
            throw new BusinessRuleViolationException(
                    "Cannot " + operation + " block in status: " + block.getStatus(), List.of(
                    Map.<String, Object>of("currentStatus", block.getStatus(), "requiredStatus", expectedStatus)));
        }
    }

    private ResourceBlock findActiveByIdOrThrow(Long id) {
        return blockRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("ResourceBlock", id));
    }

    private ResourceBlockDto toDto(ResourceBlock block) {
        return ResourceBlockDto.builder()
                .id(block.getId())
                .resourceType(block.getResourceType())
                .resourceId(block.getResourceId())
                .blockType(block.getBlockType())
                .startDate(block.getStartDate())
                .endDate(block.getEndDate())
                .startTime(block.getStartTime())
                .endTime(block.getEndTime())
                .reasonCode(block.getReasonCode())
                .reasonText(block.getReasonText())
                .recurrencePattern(block.getRecurrencePattern())
                .status(block.getStatus())
                .raisedBy(block.getRaisedBy())
                .raisedAt(block.getRaisedAt())
                .activatedAt(block.getActivatedAt())
                .releasedAt(block.getReleasedAt())
                .expiresAt(block.getExpiresAt())
                .createdAt(block.getCreatedAt())
                .updatedAt(block.getUpdatedAt())
                .build();
    }

    private static Specification<ResourceBlock> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<ResourceBlock> byResourceType(String resourceType) {
        return (root, query, cb) -> cb.equal(root.get("resourceType"), resourceType);
    }

    private static Specification<ResourceBlock> byResourceId(Long resourceId) {
        return (root, query, cb) -> cb.equal(root.get("resourceId"), resourceId);
    }

    private static Specification<ResourceBlock> byStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
