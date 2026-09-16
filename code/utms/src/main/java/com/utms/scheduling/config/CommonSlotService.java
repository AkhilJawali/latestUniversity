package com.utms.scheduling.config;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.masterdata.timeslot.SlotDefinitionRepository;
import com.utms.scheduling.engine.entity.InstitutionCommonSlot;
import com.utms.scheduling.engine.repository.InstitutionCommonSlotRepository;
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
 * CRUD service for institution common slots / CCC-UWE (A4-380).
 * Verifies campus (C-4) and slotDefinition (C-5) existence, soft-delete (C-6),
 * audit events (C-7). No uniqueness constraint in core scope (PD-82).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommonSlotService {

    private static final String ENTITY = "InstitutionCommonSlot";

    private final InstitutionCommonSlotRepository repository;
    private final CampusRepository campusRepository;
    private final SlotDefinitionRepository slotDefinitionRepository;
    private final CommonSlotMapper mapper;
    private final AuditEventPublisher auditEventPublisher;

    @Transactional
    public CommonSlotDto create(CreateCommonSlotRequest request) {
        requireCampus(request.getCampusId());
        requireSlotDefinition(request.getSlotDefinitionId());

        InstitutionCommonSlot entity = mapper.toEntity(request);
        if (entity.getAppliesToAllBatches() == null) {
            entity.setAppliesToAllBatches(true);
        }
        entity.setIsActive(true);
        entity = repository.save(entity);

        auditEventPublisher.publish(new AuditEvent(
                ENTITY, entity.getId(), AuditEvent.Action.CREATED, null, entity, "system", Instant.now()));
        log.info("Common slot created: id={}, campusId={}, name={}",
                entity.getId(), entity.getCampusId(), entity.getName());
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<CommonSlotDto> findAll(Long campusId, Pageable pageable) {
        Specification<InstitutionCommonSlot> spec = notDeleted();
        if (campusId != null) {
            Specification<InstitutionCommonSlot> byCampus = byCampusId(campusId);
            spec = spec.and(byCampus);
        }
        return repository.findAll(spec, pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public CommonSlotDto findById(Long id) {
        return mapper.toDto(findActiveOrThrow(id));
    }

    @Transactional
    public CommonSlotDto update(Long id, UpdateCommonSlotRequest request) {
        InstitutionCommonSlot entity = findActiveOrThrow(id);

        // Re-validate slot definition FK if it changed (C-5).
        if (!entity.getSlotDefinitionId().equals(request.getSlotDefinitionId())) {
            requireSlotDefinition(request.getSlotDefinitionId());
        }

        mapper.updateEntity(request, entity); // campusId ignored (PD-84)
        if (entity.getAppliesToAllBatches() == null) {
            entity.setAppliesToAllBatches(true);
        }
        entity = repository.save(entity);

        auditEventPublisher.publish(new AuditEvent(
                ENTITY, entity.getId(), AuditEvent.Action.UPDATED, null, entity, "system", Instant.now()));
        log.info("Common slot updated: id={}", entity.getId());
        return mapper.toDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        InstitutionCommonSlot entity = findActiveOrThrow(id);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setIsActive(false);
        repository.save(entity);

        auditEventPublisher.publish(new AuditEvent(
                ENTITY, entity.getId(), AuditEvent.Action.DELETED, entity, null, "system", Instant.now()));
        log.info("Common slot soft-deleted: id={}", entity.getId());
    }

    private InstitutionCommonSlot findActiveOrThrow(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY, id));
    }

    private void requireCampus(Long campusId) {
        campusRepository.findByIdAndDeletedAtIsNull(campusId)
                .orElseThrow(() -> new EntityNotFoundException("Campus", campusId));
    }

    private void requireSlotDefinition(Long slotDefinitionId) {
        slotDefinitionRepository.findByIdAndDeletedAtIsNull(slotDefinitionId)
                .orElseThrow(() -> new EntityNotFoundException("SlotDefinition", slotDefinitionId));
    }

    private static Specification<InstitutionCommonSlot> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<InstitutionCommonSlot> byCampusId(Long campusId) {
        return (root, query, cb) -> cb.equal(root.get("campusId"), campusId);
    }
}
