package com.utms.masterdata.academiccalendar;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.CampusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkingDayPatternService {

    private final WorkingDayPatternRepository patternRepository;
    private final CampusRepository campusRepository;
    private final AcademicCalendarMapper mapper;
    private final AuditEventPublisher auditEventPublisher;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public WorkingDayPatternDto create(CreateWorkingDayPatternRequest request) {
        // Validate campus exists
        if (!campusRepository.findByIdAndDeletedAtIsNull(request.getCampusId()).isPresent()) {
            throw new EntityNotFoundException("Campus", request.getCampusId());
        }

        // One pattern per campus (KD-39)
        if (patternRepository.existsByCampusIdAndDeletedAtIsNull(request.getCampusId())) {
            throw new ConflictException(
                    "A working day pattern already exists for campus ID " + request.getCampusId());
        }

        // Validate ALTERNATE_SATURDAY requires workingSaturdays
        if (request.getPatternType() == PatternType.ALTERNATE_SATURDAY
                && (request.getWorkingSaturdays() == null || request.getWorkingSaturdays().isBlank())) {
            throw new BusinessRuleViolationException(
                    "ALTERNATE_SATURDAY pattern requires workingSaturdays field (e.g., '1,3')",
                    List.of(Map.of("field", "workingSaturdays", "message", "Required for ALTERNATE_SATURDAY")));
        }

        // Validate CUSTOM requires customDefinition
        if (request.getPatternType() == PatternType.CUSTOM
                && (request.getCustomDefinition() == null || request.getCustomDefinition().isBlank())) {
            throw new BusinessRuleViolationException(
                    "CUSTOM pattern requires customDefinition field",
                    List.of(Map.of("field", "customDefinition", "message", "Required for CUSTOM")));
        }

        WorkingDayPattern pattern = new WorkingDayPattern();
        pattern.setCampus(campusRepository.getReferenceById(request.getCampusId()));
        pattern.setPatternType(request.getPatternType());
        pattern.setWorkingSaturdays(request.getWorkingSaturdays());
        pattern.setCustomDefinition(request.getCustomDefinition());
        pattern.setIsActive(true);
        pattern = patternRepository.save(pattern);

        auditEventPublisher.publish(new AuditEvent("WorkingDayPattern", pattern.getId(),
                AuditEvent.Action.CREATED, null, pattern, "system", Instant.now()));

        log.info("Working day pattern created: id={}, campusId={}, type={}",
                pattern.getId(), request.getCampusId(), request.getPatternType());
        return mapper.toPatternDto(pattern);
    }

    @Transactional(readOnly = true)
    public WorkingDayPatternDto findByCampusId(Long campusId) {
        WorkingDayPattern pattern = patternRepository.findByCampusIdAndDeletedAtIsNull(campusId)
                .orElseThrow(() -> new EntityNotFoundException("WorkingDayPattern", "campusId", campusId.toString()));
        return mapper.toPatternDto(pattern);
    }

    @Transactional
    public WorkingDayPatternDto update(Long campusId, CreateWorkingDayPatternRequest request) {
        WorkingDayPattern pattern = patternRepository.findByCampusIdAndDeletedAtIsNull(campusId)
                .orElseThrow(() -> new EntityNotFoundException("WorkingDayPattern", "campusId", campusId.toString()));

        PatternType previousType = pattern.getPatternType();
        String previousSaturdays = pattern.getWorkingSaturdays();

        // Validate ALTERNATE_SATURDAY requires workingSaturdays
        if (request.getPatternType() == PatternType.ALTERNATE_SATURDAY
                && (request.getWorkingSaturdays() == null || request.getWorkingSaturdays().isBlank())) {
            throw new BusinessRuleViolationException(
                    "ALTERNATE_SATURDAY pattern requires workingSaturdays field",
                    List.of(Map.of("field", "workingSaturdays", "message", "Required for ALTERNATE_SATURDAY")));
        }

        if (request.getPatternType() == PatternType.CUSTOM
                && (request.getCustomDefinition() == null || request.getCustomDefinition().isBlank())) {
            throw new BusinessRuleViolationException(
                    "CUSTOM pattern requires customDefinition field",
                    List.of(Map.of("field", "customDefinition", "message", "Required for CUSTOM")));
        }

        pattern.setPatternType(request.getPatternType());
        pattern.setWorkingSaturdays(request.getWorkingSaturdays());
        pattern.setCustomDefinition(request.getCustomDefinition());
        pattern = patternRepository.save(pattern);

        auditEventPublisher.publish(new AuditEvent("WorkingDayPattern", pattern.getId(),
                AuditEvent.Action.UPDATED, previousType + ":" + previousSaturdays,
                pattern.getPatternType() + ":" + pattern.getWorkingSaturdays(), "system", Instant.now()));

        // KD-38: Pattern change triggers impact detection (dates that became non-working)
        eventPublisher.publishEvent(new CalendarImpactEvent(
                campusId, LocalDate.now(), LocalDate.now().plusMonths(6),
                "PATTERN_CHANGED", "Working day pattern updated from " + previousType + " to " + request.getPatternType()));

        log.info("Working day pattern updated: campusId={}, type={}", campusId, request.getPatternType());
        return mapper.toPatternDto(pattern);
    }

    /**
     * KD-39: Pattern removal is BLOCKED — a pattern is required per campus.
     * Returns 422 if attempted.
     */
    @Transactional
    public void delete(Long campusId) {
        throw new BusinessRuleViolationException(
                "Cannot remove working day pattern: every campus must have a pattern configured (KD-39)",
                List.of(Map.of("rule", "KD-39", "campusId", campusId.toString(),
                        "message", "Update the pattern instead of removing it")));
    }
}
