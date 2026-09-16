package com.utms.scheduling.config;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.scheduling.engine.entity.SessionDerivationRule;
import com.utms.scheduling.engine.repository.SessionDerivationRuleRepository;
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
 * CRUD service for session-derivation rules (A4-380).
 * Mirrors the master-data Asset CRUD pattern: campus existence check,
 * (campus, componentType) uniqueness (C-1), soft-delete (C-6), audit events (C-7).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DerivationRuleService {

    private static final String ENTITY = "SessionDerivationRule";

    private final SessionDerivationRuleRepository repository;
    private final CampusRepository campusRepository;
    private final DerivationRuleMapper mapper;
    private final AuditEventPublisher auditEventPublisher;

    @Transactional
    public DerivationRuleDto create(CreateDerivationRuleRequest request) {
        requireCampus(request.getCampusId());
        requireUniqueComponentType(request.getCampusId(), request.getComponentType());

        SessionDerivationRule entity = mapper.toEntity(request);
        entity.setIsActive(true);
        entity = repository.save(entity);

        auditEventPublisher.publish(new AuditEvent(
                ENTITY, entity.getId(), AuditEvent.Action.CREATED, null, entity, "system", Instant.now()));
        log.info("Derivation rule created: id={}, campusId={}, componentType={}",
                entity.getId(), entity.getCampusId(), entity.getComponentType());
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<DerivationRuleDto> findAll(Long campusId, Pageable pageable) {
        Specification<SessionDerivationRule> spec = notDeleted();
        if (campusId != null) {
            Specification<SessionDerivationRule> byCampus = byCampusId(campusId);
            spec = spec.and(byCampus);
        }
        return repository.findAll(spec, pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public DerivationRuleDto findById(Long id) {
        return mapper.toDto(findActiveOrThrow(id));
    }

    @Transactional
    public DerivationRuleDto update(Long id, UpdateDerivationRuleRequest request) {
        SessionDerivationRule entity = findActiveOrThrow(id);

        // Uniqueness re-check only when componentType changes (C-1), scoped to the record's campus.
        if (!entity.getComponentType().equals(request.getComponentType())) {
            requireUniqueComponentType(entity.getCampusId(), request.getComponentType());
        }

        mapper.updateEntity(request, entity); // campusId ignored (PD-84)
        // isActive is a BaseEntity field ignored by the mapper config; apply it
        // explicitly so the admin can toggle a rule active/inactive on edit.
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
        entity = repository.save(entity);

        auditEventPublisher.publish(new AuditEvent(
                ENTITY, entity.getId(), AuditEvent.Action.UPDATED, null, entity, "system", Instant.now()));
        log.info("Derivation rule updated: id={}", entity.getId());
        return mapper.toDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        SessionDerivationRule entity = findActiveOrThrow(id);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setIsActive(false);
        repository.save(entity);

        auditEventPublisher.publish(new AuditEvent(
                ENTITY, entity.getId(), AuditEvent.Action.DELETED, entity, null, "system", Instant.now()));
        log.info("Derivation rule soft-deleted: id={}", entity.getId());
    }

    private SessionDerivationRule findActiveOrThrow(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY, id));
    }

    private void requireCampus(Long campusId) {
        campusRepository.findByIdAndDeletedAtIsNull(campusId)
                .orElseThrow(() -> new EntityNotFoundException("Campus", campusId));
    }

    private void requireUniqueComponentType(Long campusId, String componentType) {
        if (repository.existsByCampusIdAndComponentTypeAndDeletedAtIsNull(campusId, componentType)) {
            throw new ConflictException("Derivation rule for component type '" + componentType
                    + "' already exists for this campus");
        }
    }

    private static Specification<SessionDerivationRule> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<SessionDerivationRule> byCampusId(Long campusId) {
        return (root, query, cb) -> cb.equal(root.get("campusId"), campusId);
    }
}
