package com.utms.masterdata.program;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.batch.BatchRepository;
import com.utms.masterdata.department.Department;
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
public class ProgramService {

    private final ProgramRepository programRepository;
    private final ProgramMapper programMapper;
    private final DepartmentRepository departmentRepository;
    private final BatchRepository batchRepository;
    private final AuditEventPublisher auditEventPublisher;

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public ProgramDto create(CreateProgramRequest request) {
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(request.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department", request.getDepartmentId()));

        if (programRepository.existsByCodeAndDepartmentIdAndDeletedAtIsNull(request.getCode(), request.getDepartmentId())) {
            throw new ConflictException("Program with code '" + request.getCode() + "' already exists in this department");
        }

        Program program = programMapper.toEntity(request);
        program.setDepartment(department);
        program.setIsActive(true);
        program = programRepository.save(program);

        auditEventPublisher.publish(new AuditEvent("Program", program.getId(), AuditEvent.Action.CREATED, null, program, "system", Instant.now()));

        log.info("Program created: id={}, code={}, departmentId={}", program.getId(), program.getCode(), department.getId());
        return programMapper.toDto(program);
    }

    @Transactional(readOnly = true)
    public ProgramDto findById(Long id) {
        Program program = findActiveByIdOrThrow(id);
        return programMapper.toDto(program);
    }

    @Transactional(readOnly = true)
    public Page<ProgramDto> findAll(Long departmentId, Pageable pageable) {
        Specification<Program> spec = notDeleted();
        if (departmentId != null) {
            Specification<Program> deptSpec = byDepartmentId(departmentId);
            spec = Specification.where(spec).and(deptSpec);
        }
        return programRepository.findAll(spec, pageable)
                .map(programMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public ProgramDto update(Long id, UpdateProgramRequest request) {
        Program program = findActiveByIdOrThrow(id);

        String previousName = program.getName();
        programMapper.updateEntity(request, program);
        program = programRepository.save(program);

        auditEventPublisher.publish(new AuditEvent("Program", program.getId(), AuditEvent.Action.UPDATED, previousName, program, "system", Instant.now()));

        log.info("Program updated: id={}", program.getId());
        return programMapper.toDto(program);
    }

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public void delete(Long id) {
        Program program = findActiveByIdOrThrow(id);

        List<Map<String, Object>> references = new ArrayList<>();
        long batchCount = batchRepository.countByProgramIdAndDeletedAtIsNull(id);
        if (batchCount > 0) {
            references.add(Map.of("referenceType", "Batch", "count", batchCount));
        }

        if (!references.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot delete program: has active references", references);
        }

        program.setDeletedAt(LocalDateTime.now());
        program.setIsActive(false);
        programRepository.save(program);

        auditEventPublisher.publish(new AuditEvent("Program", program.getId(), AuditEvent.Action.DELETED, program, null, "system", Instant.now()));

        log.info("Program soft-deleted: id={}, code={}", program.getId(), program.getCode());
    }

    private Program findActiveByIdOrThrow(Long id) {
        return programRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Program", id));
    }

    private static Specification<Program> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<Program> byDepartmentId(Long departmentId) {
        return (root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId);
    }
}
