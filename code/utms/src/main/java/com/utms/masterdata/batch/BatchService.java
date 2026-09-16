package com.utms.masterdata.batch;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.program.Program;
import com.utms.masterdata.program.ProgramRepository;
import com.utms.masterdata.section.SectionRepository;
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
public class BatchService {

    private final BatchRepository batchRepository;
    private final BatchMapper batchMapper;
    private final ProgramRepository programRepository;
    private final SectionRepository sectionRepository;
    private final AuditEventPublisher auditEventPublisher;

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public BatchDto create(CreateBatchRequest request) {
        Program program = programRepository.findByIdAndDeletedAtIsNull(request.getProgramId())
                .orElseThrow(() -> new EntityNotFoundException("Program", request.getProgramId()));

        Batch batch = batchMapper.toEntity(request);
        batch.setProgram(program);
        batch.setIsActive(true);
        batch = batchRepository.save(batch);

        auditEventPublisher.publish(new AuditEvent("Batch", batch.getId(), AuditEvent.Action.CREATED, null, batch, "system", Instant.now()));

        log.info("Batch created: id={}, yearIdentifier={}, programId={}", batch.getId(), batch.getYearIdentifier(), program.getId());
        return batchMapper.toDto(batch);
    }

    @Transactional(readOnly = true)
    public BatchDto findById(Long id) {
        Batch batch = findActiveByIdOrThrow(id);
        return batchMapper.toDto(batch);
    }

    @Transactional(readOnly = true)
    public Page<BatchDto> findAll(Long programId, Pageable pageable) {
        Specification<Batch> spec = notDeleted();
        if (programId != null) {
            Specification<Batch> programSpec = byProgramId(programId);
            spec = Specification.where(spec).and(programSpec);
        }
        return batchRepository.findAll(spec, pageable)
                .map(batchMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public BatchDto update(Long id, UpdateBatchRequest request) {
        Batch batch = findActiveByIdOrThrow(id);

        // FIX 2: Guard against reducing strength below existing section sub-strengths
        if (request.getStrength() != null && request.getStrength() > 0) {
            Integer maxSubStrength = sectionRepository.findMaxSubStrengthByBatchId(batch.getId());
            if (maxSubStrength != null && request.getStrength() < maxSubStrength) {
                throw new BusinessRuleViolationException(
                    "Cannot reduce batch strength to " + request.getStrength()
                    + ": section sub-strength of " + maxSubStrength + " would be exceeded");
            }
        }

        String previousYearIdentifier = batch.getYearIdentifier();
        batchMapper.updateEntity(request, batch);
        batch = batchRepository.save(batch);

        auditEventPublisher.publish(new AuditEvent("Batch", batch.getId(), AuditEvent.Action.UPDATED, previousYearIdentifier, batch, "system", Instant.now()));

        log.info("Batch updated: id={}", batch.getId());
        return batchMapper.toDto(batch);
    }

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public void delete(Long id) {
        Batch batch = findActiveByIdOrThrow(id);

        List<Map<String, Object>> references = new ArrayList<>();
        long sectionCount = sectionRepository.countByBatchIdAndDeletedAtIsNull(id);
        if (sectionCount > 0) {
            references.add(Map.of("referenceType", "Section", "count", sectionCount));
        }

        if (!references.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot delete batch: has active references", references);
        }

        batch.setDeletedAt(LocalDateTime.now());
        batch.setIsActive(false);
        batchRepository.save(batch);

        auditEventPublisher.publish(new AuditEvent("Batch", batch.getId(), AuditEvent.Action.DELETED, batch, null, "system", Instant.now()));

        log.info("Batch soft-deleted: id={}, yearIdentifier={}", batch.getId(), batch.getYearIdentifier());
    }

    private Batch findActiveByIdOrThrow(Long id) {
        return batchRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Batch", id));
    }

    private static Specification<Batch> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<Batch> byProgramId(Long programId) {
        return (root, query, cb) -> cb.equal(root.get("program").get("id"), programId);
    }
}
