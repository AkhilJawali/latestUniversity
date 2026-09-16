package com.utms.scheduling.config;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.scheduling.engine.entity.SoftConstraintWeight;
import com.utms.scheduling.engine.repository.SoftConstraintWeightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * CRUD service for soft-constraint weights (A4-380).
 * (campus, constraintType) uniqueness (C-2), constraintType validated at DTO layer (C-3),
 * soft-delete (C-6), audit events (C-7).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SoftConstraintWeightService {

    private static final String ENTITY = "SoftConstraintWeight";

    private final SoftConstraintWeightRepository repository;
    private final CampusRepository campusRepository;
    private final SoftConstraintWeightMapper mapper;
    private final AuditEventPublisher auditEventPublisher;

    @Transactional
    public SoftConstraintWeightDto create(CreateSoftConstraintWeightRequest request) {
        requireCampus(request.getCampusId());
        requireUniqueConstraintType(request.getCampusId(), request.getConstraintType());

        SoftConstraintWeight entity = mapper.toEntity(request);
        entity.setIsActive(true);
        entity = repository.save(entity);

        auditEventPublisher.publish(new AuditEvent(
                ENTITY, entity.getId(), AuditEvent.Action.CREATED, null, entity, "system", Instant.now()));
        log.info("Soft-constraint weight created: id={}, campusId={}, constraintType={}",
                entity.getId(), entity.getCampusId(), entity.getConstraintType());
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<SoftConstraintWeightDto> findAll(Long campusId, Pageable pageable) {
        Specification<SoftConstraintWeight> spec = notDeleted();
        if (campusId != null) {
            Specification<SoftConstraintWeight> byCampus = byCampusId(campusId);
            spec = spec.and(byCampus);
        }
        return repository.findAll(spec, pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public SoftConstraintWeightDto findById(Long id) {
        return mapper.toDto(findActiveOrThrow(id));
    }

    @Transactional
    public SoftConstraintWeightDto update(Long id, UpdateSoftConstraintWeightRequest request) {
        SoftConstraintWeight entity = findActiveOrThrow(id);

        if (!entity.getConstraintType().equals(request.getConstraintType())) {
            requireUniqueConstraintType(entity.getCampusId(), request.getConstraintType());
        }

        mapper.updateEntity(request, entity); // campusId ignored (PD-84)
        entity = repository.save(entity);

        auditEventPublisher.publish(new AuditEvent(
                ENTITY, entity.getId(), AuditEvent.Action.UPDATED, null, entity, "system", Instant.now()));
        log.info("Soft-constraint weight updated: id={}", entity.getId());
        return mapper.toDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        SoftConstraintWeight entity = findActiveOrThrow(id);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setIsActive(false);
        repository.save(entity);

        auditEventPublisher.publish(new AuditEvent(
                ENTITY, entity.getId(), AuditEvent.Action.DELETED, entity, null, "system", Instant.now()));
        log.info("Soft-constraint weight soft-deleted: id={}", entity.getId());
    }

    private SoftConstraintWeight findActiveOrThrow(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY, id));
    }

    private void requireCampus(Long campusId) {
        campusRepository.findByIdAndDeletedAtIsNull(campusId)
                .orElseThrow(() -> new EntityNotFoundException("Campus", campusId));
    }

    private void requireUniqueConstraintType(Long campusId, String constraintType) {
        if (repository.existsByCampusIdAndConstraintTypeAndDeletedAtIsNull(campusId, constraintType)) {
            throw new ConflictException("Weight for constraint type '" + constraintType
                    + "' already exists for this campus");
        }
    }

    private static Specification<SoftConstraintWeight> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<SoftConstraintWeight> byCampusId(Long campusId) {
        return (root, query, cb) -> cb.equal(root.get("campusId"), campusId);
    }
}
