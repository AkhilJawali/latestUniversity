package com.utms.masterdata.campus;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.department.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampusService {

    private final CampusRepository campusRepository;
    private final CampusMapper campusMapper;
    private final DepartmentRepository departmentRepository;
    private final AuditEventPublisher auditEventPublisher;

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public CampusDto create(CreateCampusRequest request) {
        if (campusRepository.existsByCodeAndDeletedAtIsNull(request.getCode())) {
            throw new ConflictException("Campus with code '" + request.getCode() + "' already exists");
        }

        Campus campus = campusMapper.toEntity(request);
        campus.setIsActive(true);
        campus = campusRepository.save(campus);

        auditEventPublisher.publish(new AuditEvent("Campus", campus.getId(), AuditEvent.Action.CREATED, null, campus, "system", Instant.now()));

        log.info("Campus created: id={}, code={}", campus.getId(), campus.getCode());
        return campusMapper.toDto(campus);
    }

    @Transactional(readOnly = true)
    public CampusDto findById(Long id) {
        Campus campus = findActiveByIdOrThrow(id);
        return campusMapper.toDto(campus);
    }

    @Transactional(readOnly = true)
    public CampusDto findByCode(String code) {
        Campus campus = campusRepository.findByCodeAndDeletedAtIsNull(code)
                .orElseThrow(() -> new EntityNotFoundException("Campus", "code", code));
        return campusMapper.toDto(campus);
    }

    @Transactional(readOnly = true)
    public Page<CampusDto> findAll(Pageable pageable) {
        Specification<Campus> spec = notDeleted();
        return campusRepository.findAll(spec, pageable)
                .map(campusMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public CampusDto update(Long id, UpdateCampusRequest request) {
        Campus campus = findActiveByIdOrThrow(id);

        String previousName = campus.getName();
        campusMapper.updateEntity(request, campus);
        campus = campusRepository.save(campus);

        auditEventPublisher.publish(new AuditEvent("Campus", campus.getId(), AuditEvent.Action.UPDATED, previousName, campus, "system", Instant.now()));

        log.info("Campus updated: id={}", campus.getId());
        return campusMapper.toDto(campus);
    }

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public void delete(Long id) {
        Campus campus = findActiveByIdOrThrow(id);

        // Check active references only (KD-23: historical do NOT block)
        List<Map<String, Object>> references = new ArrayList<>();
        long deptCount = departmentRepository.countByCampusIdAndDeletedAtIsNull(id);
        if (deptCount > 0) {
            references.add(Map.of("referenceType", "Department", "count", deptCount));
        }

        if (!references.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot delete campus: has active references", references);
        }

        campus.setDeletedAt(LocalDateTime.now());
        campus.setIsActive(false);
        campusRepository.save(campus);

        auditEventPublisher.publish(new AuditEvent("Campus", campus.getId(), AuditEvent.Action.DELETED, campus, null, "system", Instant.now()));

        log.info("Campus soft-deleted: id={}, code={}", campus.getId(), campus.getCode());
    }

    private Campus findActiveByIdOrThrow(Long id) {
        return campusRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Campus", id));
    }

    private static Specification<Campus> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
