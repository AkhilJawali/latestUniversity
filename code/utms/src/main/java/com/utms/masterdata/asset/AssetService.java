package com.utms.masterdata.asset;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetService {

    private final SchedulableAssetRepository assetRepository;
    private final AssetMapper assetMapper;
    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;
    private final AuditEventPublisher auditEventPublisher;

    @Transactional
    public AssetDto create(CreateAssetRequest request) {
        // Validate department exists
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(request.getOwningDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department", request.getOwningDepartmentId()));

        // Validate campus exists
        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(request.getCampusId())
                .orElseThrow(() -> new EntityNotFoundException("Campus", request.getCampusId()));

        // Check duplicate identifier (partial unique — active only)
        if (assetRepository.existsByIdentifierAndDeletedAtIsNull(request.getIdentifier())) {
            throw new ConflictException("Asset with identifier '" + request.getIdentifier() + "' already exists");
        }

        SchedulableAsset asset = assetMapper.toEntity(request);
        asset.setOwningDepartment(department);
        asset.setCampus(campus);
        asset.setIsActive(true);

        // Create availability windows inline (KD-28)
        if (request.getAvailabilityWindows() != null) {
            for (CreateAvailabilityWindowRequest windowReq : request.getAvailabilityWindows()) {
                AssetAvailabilityWindow window = new AssetAvailabilityWindow();
                window.setDayOfWeek(windowReq.getDayOfWeek());
                window.setStartTime(windowReq.getStartTime());
                window.setEndTime(windowReq.getEndTime());
                window.setIsActive(true);
                window.setAsset(asset);
                asset.getAvailabilityWindows().add(window);
            }
        }

        asset = assetRepository.save(asset);

        auditEventPublisher.publish(new AuditEvent(
                "SchedulableAsset", asset.getId(), AuditEvent.Action.CREATED, null, asset, "system", Instant.now()));

        log.info("Asset created: id={}, identifier={}, campusId={}", asset.getId(), asset.getIdentifier(), campus.getId());
        return assetMapper.toDto(asset);
    }

    @Transactional(readOnly = true)
    public AssetDto findById(Long id) {
        SchedulableAsset asset = findActiveByIdOrThrow(id);
        return assetMapper.toDto(asset);
    }

    @Transactional(readOnly = true)
    public Page<AssetDto> findAll(Long campusId, Long departmentId, String assetType, Pageable pageable) {
        Specification<SchedulableAsset> spec = notDeleted();

        if (campusId != null) {
            Specification<SchedulableAsset> campusSpec = byCampusId(campusId);
            spec = spec.and(campusSpec);
        }
        if (departmentId != null) {
            Specification<SchedulableAsset> deptSpec = byDepartmentId(departmentId);
            spec = spec.and(deptSpec);
        }
        if (assetType != null && !assetType.isBlank()) {
            Specification<SchedulableAsset> typeSpec = byAssetType(assetType);
            spec = spec.and(typeSpec);
        }

        return assetRepository.findAll(spec, pageable).map(assetMapper::toDto);
    }

    @Transactional
    public AssetDto update(Long id, UpdateAssetRequest request) {
        SchedulableAsset asset = findActiveByIdOrThrow(id);

        assetMapper.updateEntity(request, asset);

        // Replace availability windows
        if (request.getAvailabilityWindows() != null) {
            asset.getAvailabilityWindows().clear();
            for (CreateAvailabilityWindowRequest windowReq : request.getAvailabilityWindows()) {
                AssetAvailabilityWindow window = new AssetAvailabilityWindow();
                window.setDayOfWeek(windowReq.getDayOfWeek());
                window.setStartTime(windowReq.getStartTime());
                window.setEndTime(windowReq.getEndTime());
                window.setIsActive(true);
                window.setAsset(asset);
                asset.getAvailabilityWindows().add(window);
            }
        }

        asset = assetRepository.save(asset);

        auditEventPublisher.publish(new AuditEvent(
                "SchedulableAsset", asset.getId(), AuditEvent.Action.UPDATED, null, asset, "system", Instant.now()));

        log.info("Asset updated: id={}", asset.getId());
        return assetMapper.toDto(asset);
    }

    @Transactional
    public void delete(Long id) {
        SchedulableAsset asset = findActiveByIdOrThrow(id);

        // Check active resource blocks only — historical blocks do NOT block deletion (KD-30)
        List<java.util.Map<String, Object>> references = new ArrayList<>();
        // TODO: Check active resource blocks referencing this asset once A4-8 (resource block module) is built
        // long activeBlockCount = resourceBlockRepository.countByAssetIdAndEndDateAfterAndDeletedAtIsNull(id, LocalDate.now());
        // if (activeBlockCount > 0) {
        //     references.add(Map.of("referenceType", "ResourceBlock", "count", activeBlockCount));
        // }

        if (!references.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot delete asset: has active resource blocks", references);
        }

        asset.setDeletedAt(LocalDateTime.now());
        asset.setIsActive(false);
        assetRepository.save(asset);

        auditEventPublisher.publish(new AuditEvent(
                "SchedulableAsset", asset.getId(), AuditEvent.Action.DELETED, asset, null, "system", Instant.now()));

        log.info("Asset soft-deleted: id={}, identifier={}", asset.getId(), asset.getIdentifier());
    }

    private SchedulableAsset findActiveByIdOrThrow(Long id) {
        return assetRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("SchedulableAsset", id));
    }

    private static Specification<SchedulableAsset> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<SchedulableAsset> byCampusId(Long campusId) {
        return (root, query, cb) -> cb.equal(root.get("campus").get("id"), campusId);
    }

    private static Specification<SchedulableAsset> byDepartmentId(Long departmentId) {
        return (root, query, cb) -> cb.equal(root.get("owningDepartment").get("id"), departmentId);
    }

    private static Specification<SchedulableAsset> byAssetType(String assetType) {
        return (root, query, cb) -> cb.equal(root.get("assetType"), assetType);
    }
}
