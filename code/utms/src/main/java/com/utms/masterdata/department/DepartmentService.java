package com.utms.masterdata.department;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.masterdata.program.ProgramRepository;
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
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final CampusRepository campusRepository;
    private final ProgramRepository programRepository;
    private final AuditEventPublisher auditEventPublisher;

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public DepartmentDto create(CreateDepartmentRequest request) {
        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(request.getCampusId())
                .orElseThrow(() -> new EntityNotFoundException("Campus", request.getCampusId()));

        if (departmentRepository.existsByCodeAndCampusIdAndDeletedAtIsNull(request.getCode(), request.getCampusId())) {
            throw new ConflictException("Department with code '" + request.getCode() + "' already exists in this campus");
        }

        Department department = departmentMapper.toEntity(request);
        department.setCampus(campus);
        department.setIsActive(true);
        department = departmentRepository.save(department);

        auditEventPublisher.publish(new AuditEvent("Department", department.getId(), AuditEvent.Action.CREATED, null, department, "system", Instant.now()));

        log.info("Department created: id={}, code={}, campusId={}", department.getId(), department.getCode(), campus.getId());
        return departmentMapper.toDto(department);
    }

    @Transactional(readOnly = true)
    public DepartmentDto findById(Long id) {
        Department department = findActiveByIdOrThrow(id);
        return departmentMapper.toDto(department);
    }

    @Transactional(readOnly = true)
    public Page<DepartmentDto> findAll(Long campusId, Pageable pageable) {
        Specification<Department> spec = notDeleted();
        if (campusId != null) {
            Specification<Department> campusSpec = byCampusId(campusId);
            spec = Specification.where(spec).and(campusSpec);
        }
        return departmentRepository.findAll(spec, pageable)
                .map(departmentMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public DepartmentDto update(Long id, UpdateDepartmentRequest request) {
        Department department = findActiveByIdOrThrow(id);

        String previousName = department.getName();
        departmentMapper.updateEntity(request, department);
        department = departmentRepository.save(department);

        auditEventPublisher.publish(new AuditEvent("Department", department.getId(), AuditEvent.Action.UPDATED, previousName, department, "system", Instant.now()));

        log.info("Department updated: id={}", department.getId());
        return departmentMapper.toDto(department);
    }

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public void delete(Long id) {
        Department department = findActiveByIdOrThrow(id);

        List<Map<String, Object>> references = new ArrayList<>();
        long programCount = programRepository.countByDepartmentIdAndDeletedAtIsNull(id);
        if (programCount > 0) {
            references.add(Map.of("referenceType", "Program", "count", programCount));
        }

        if (!references.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot delete department: has active references", references);
        }

        department.setDeletedAt(LocalDateTime.now());
        department.setIsActive(false);
        departmentRepository.save(department);

        auditEventPublisher.publish(new AuditEvent("Department", department.getId(), AuditEvent.Action.DELETED, department, null, "system", Instant.now()));

        log.info("Department soft-deleted: id={}, code={}", department.getId(), department.getCode());
    }

    private Department findActiveByIdOrThrow(Long id) {
        return departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Department", id));
    }

    private static Specification<Department> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<Department> byCampusId(Long campusId) {
        return (root, query, cb) -> cb.equal(root.get("campus").get("id"), campusId);
    }
}
