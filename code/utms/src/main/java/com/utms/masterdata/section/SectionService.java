package com.utms.masterdata.section;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.batch.Batch;
import com.utms.masterdata.batch.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectionService {

    private final SectionRepository sectionRepository;
    private final SectionMapper sectionMapper;
    private final BatchRepository batchRepository;
    private final AuditEventPublisher auditEventPublisher;

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public SectionDto create(Long batchId, CreateSectionRequest request) {
        Batch batch = batchRepository.findByIdAndDeletedAtIsNull(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch", batchId));

        if (sectionRepository.existsBySectionIdentifierAndBatchIdAndDeletedAtIsNull(
                request.getSectionIdentifier(), batchId)) {
            throw new ConflictException("Section with identifier '" + request.getSectionIdentifier()
                    + "' already exists in this batch");
        }

        // The TOTAL of all sections' sub-strengths must not exceed the batch strength.
        if (request.getSubStrength() != null) {
            int existingTotal = sumSubStrength(batchId, null);
            int newTotal = existingTotal + request.getSubStrength();
            if (newTotal > batch.getStrength()) {
                throw new BusinessRuleViolationException(
                        "Total section sub-strength (" + newTotal + ") would exceed the batch strength ("
                                + batch.getStrength() + "). Already allocated: " + existingTotal + ".");
            }
        }

        Section section = sectionMapper.toEntity(request);
        section.setBatch(batch);
        section.setIsActive(true);
        section = sectionRepository.save(section);

        auditEventPublisher.publish(new AuditEvent("Section", section.getId(), AuditEvent.Action.CREATED, null, section, "system", Instant.now()));

        log.info("Section created: id={}, identifier={}, batchId={}", section.getId(), section.getSectionIdentifier(), batchId);
        return sectionMapper.toDto(section);
    }

    @Transactional(readOnly = true)
    public SectionDto findById(Long id) {
        Section section = findActiveByIdOrThrow(id);
        return sectionMapper.toDto(section);
    }

    @Transactional(readOnly = true)
    public List<SectionDto> findAllByBatchId(Long batchId) {
        if (!batchRepository.findByIdAndDeletedAtIsNull(batchId).isPresent()) {
            throw new EntityNotFoundException("Batch", batchId);
        }
        return sectionRepository.findAllByBatchIdAndDeletedAtIsNull(batchId)
                .stream()
                .map(sectionMapper::toDto)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public SectionDto update(Long id, UpdateSectionRequest request) {
        Section section = findActiveByIdOrThrow(id);

        if (request.getSubStrength() != null) {
            int batchStrength = section.getBatch().getStrength();
            // Sum of the OTHER sections (exclude this one) plus the new value.
            int othersTotal = sumSubStrength(section.getBatch().getId(), section.getId());
            int newTotal = othersTotal + request.getSubStrength();
            if (newTotal > batchStrength) {
                throw new BusinessRuleViolationException(
                        "Total section sub-strength (" + newTotal + ") would exceed the batch strength ("
                                + batchStrength + "). Allocated by other sections: " + othersTotal + ".");
            }
        }

        String previousIdentifier = section.getSectionIdentifier();
        sectionMapper.updateEntity(request, section);
        section = sectionRepository.save(section);

        auditEventPublisher.publish(new AuditEvent("Section", section.getId(), AuditEvent.Action.UPDATED, previousIdentifier, section, "system", Instant.now()));

        log.info("Section updated: id={}", section.getId());
        return sectionMapper.toDto(section);
    }

    @Transactional
    @CacheEvict(value = "hierarchyTree", allEntries = true)
    public void delete(Long id) {
        Section section = findActiveByIdOrThrow(id);

        // No child checks in Phase 1 — sessions TODO
        section.setDeletedAt(LocalDateTime.now());
        section.setIsActive(false);
        sectionRepository.save(section);

        auditEventPublisher.publish(new AuditEvent("Section", section.getId(), AuditEvent.Action.DELETED, section, null, "system", Instant.now()));

        log.info("Section soft-deleted: id={}, identifier={}", section.getId(), section.getSectionIdentifier());
    }

    private Section findActiveByIdOrThrow(Long id) {
        return sectionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Section", id));
    }

    /**
     * Total sub-strength of all active sections in a batch. When {@code excludeId} is
     * non-null the section with that id is excluded (used on update so re-saving the
     * same section does not double-count its own value). Null sub-strengths count as 0.
     */
    private int sumSubStrength(Long batchId, Long excludeId) {
        return sectionRepository.findAllByBatchIdAndDeletedAtIsNull(batchId).stream()
                .filter(s -> excludeId == null || !s.getId().equals(excludeId))
                .mapToInt(s -> s.getSubStrength() != null ? s.getSubStrength() : 0)
                .sum();
    }
}
