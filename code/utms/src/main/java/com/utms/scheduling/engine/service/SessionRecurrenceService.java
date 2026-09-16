package com.utms.scheduling.engine.service;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.academiccalendar.AcademicCalendar;
import com.utms.masterdata.academiccalendar.AcademicCalendarRepository;
import com.utms.masterdata.academiccalendar.CalendarQueryService;
import com.utms.scheduling.engine.dto.OccurrenceDatesDto;
import com.utms.scheduling.engine.dto.SessionRecurrenceDto;
import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.entity.TimetableDraft;
import com.utms.scheduling.engine.enums.RecurrenceType;
import com.utms.scheduling.engine.enums.WeekGroup;
import com.utms.scheduling.engine.repository.ScheduledSessionRepository;
import com.utms.scheduling.engine.repository.TimetableDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns setting/reading a session's recurrence pattern (A4-13, design sections 5.3-5.5).
 *
 * <p>Provides the write path (set/revert a pattern, audited) and the read path used by
 * downstream consumers — the occurrence-date enumeration that the calendar feed (A4-39)
 * and timetable display (A4-15) will call. The alternate-week non-conflict rule itself
 * lives in {@link RecurrenceOverlapEvaluator}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionRecurrenceService {

    private static final String ENTITY_TYPE = "ScheduledSession";

    private final ScheduledSessionRepository sessionRepository;
    private final TimetableDraftRepository draftRepository;
    private final AcademicCalendarRepository calendarRepository;
    private final CalendarQueryService calendarQueryService;
    private final WeekParityResolver parityResolver;
    private final AuditEventPublisher auditPublisher;

    /**
     * Sets or changes a session's recurrence pattern (FR-1.1, FR-1.2, FR-1.4, FR-1.5).
     * Only the recurrence columns are mutated — the session's day and slot are never
     * touched (FR-1.5, KD-64). Reverting to WEEKLY clears the week group.
     *
     * @throws EntityNotFoundException          if the session does not exist (404)
     * @throws BusinessRuleViolationException   if the type/group combination is invalid (422, HC-FN-4)
     */
    @Transactional
    public SessionRecurrenceDto setRecurrence(Long sessionId, RecurrenceType recurrenceType,
                                              WeekGroup weekGroup, String actor) {
        ScheduledSession session = sessionRepository.findByIdAndDeletedAtIsNull(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_TYPE, sessionId));

        validate(recurrenceType, weekGroup);

        Map<String, Object> before = snapshot(session);

        session.setRecurrenceType(recurrenceType);
        // KD-64: a weekly session has no week group; reverting clears it.
        session.setWeekGroup(recurrenceType == RecurrenceType.FORTNIGHTLY ? weekGroup : null);
        ScheduledSession saved = sessionRepository.save(session);

        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, saved.getId(), AuditEvent.Action.UPDATED,
                before, snapshot(saved), actor, Instant.now()));

        log.info("Recurrence updated: sessionId={}, type={}, weekGroup={}",
                saved.getId(), saved.getRecurrenceType(), saved.getWeekGroup());
        return toDto(saved);
    }

    /**
     * Reverts a session to weekly recurrence (FR-1.4).
     */
    @Transactional
    public SessionRecurrenceDto revertToWeekly(Long sessionId, String actor) {
        return setRecurrence(sessionId, RecurrenceType.WEEKLY, null, actor);
    }

    /**
     * Lists the actual dates a session occurs within the semester (FR-4.2, FR-6.1, FR-6.2).
     *
     * <p>Enumerates every date in the semester matching the session's day of week, filters
     * to the assigned week group for fortnightly sessions, and drops holidays / non-working
     * days via {@link CalendarQueryService#isWorkingDay} (PD-82: skip, no shift). A weekly
     * session yields every working occurrence of its day (feed behavior unchanged, FR-6.2).</p>
     *
     * @throws EntityNotFoundException        if the session does not exist (404)
     * @throws BusinessRuleViolationException if no academic calendar anchor exists (422, CALENDAR_ANCHOR)
     */
    @Transactional(readOnly = true)
    public OccurrenceDatesDto getOccurrenceDates(Long sessionId) {
        ScheduledSession session = sessionRepository.findByIdAndDeletedAtIsNull(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_TYPE, sessionId));

        TimetableDraft draft = draftRepository.findByIdAndDeletedAtIsNull(session.getDraftId())
                .orElseThrow(() -> new EntityNotFoundException("TimetableDraft", session.getDraftId()));

        Long campusId = resolveCampusId(draft.getDepartmentId());
        AcademicCalendar calendar = resolveCalendarAnchor(campusId, draft.getAcademicYear(), draft.getSemester());

        LocalDate semesterStart = calendar.getSemesterStartDate();
        LocalDate semesterEnd = calendar.getSemesterEndDate();
        if (semesterEnd.isBefore(semesterStart)) {
            throw new BusinessRuleViolationException(
                    "Academic calendar is misconfigured: semester end precedes semester start for campus "
                            + campusId,
                    List.of(Map.of(
                            "check", "CALENDAR_ANCHOR",
                            "message", "semesterEndDate must not precede semesterStartDate")));
        }
        DayOfWeek targetDow = DayOfWeek.valueOf(session.getDayOfWeek().toUpperCase());

        List<LocalDate> occurrences = new ArrayList<>();
        for (LocalDate d = semesterStart; !d.isAfter(semesterEnd); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != targetDow) {
                continue;
            }
            if (session.getRecurrenceType() == RecurrenceType.FORTNIGHTLY
                    && parityResolver.resolve(d, semesterStart) != session.getWeekGroup()) {
                continue;
            }
            if (!calendarQueryService.isWorkingDay(campusId, d)) {
                continue; // PD-82: holiday/non-working occurrence skipped, no shift
            }
            occurrences.add(d);
        }

        return OccurrenceDatesDto.builder()
                .sessionId(session.getId())
                .recurrenceType(session.getRecurrenceType().name())
                .weekGroup(session.getWeekGroup() != null ? session.getWeekGroup().name() : null)
                .occurrenceDates(occurrences)
                .build();
    }

    // ===== Private helpers =====

    /**
     * HC-FN-4: WEEKLY requires no week group; FORTNIGHTLY requires exactly one.
     */
    private void validate(RecurrenceType recurrenceType, WeekGroup weekGroup) {
        boolean invalid = (recurrenceType == RecurrenceType.WEEKLY && weekGroup != null)
                || (recurrenceType == RecurrenceType.FORTNIGHTLY && weekGroup == null);
        if (invalid) {
            throw new BusinessRuleViolationException(
                    "Invalid recurrence pattern",
                    List.of(Map.of(
                            "field", "weekGroup",
                            "message", "weekGroup is required when recurrenceType is FORTNIGHTLY "
                                    + "and must be null when WEEKLY")));
        }
    }

    private AcademicCalendar resolveCalendarAnchor(Long campusId, String academicYear, String semesterIdentifier) {
        return calendarRepository
                .findByCampusIdAndAcademicYearAndSemesterIdentifierAndDeletedAtIsNull(
                        campusId, academicYear, semesterIdentifier)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Cannot compute week parity: no academic calendar with a semester start date for campus "
                                + campusId,
                        List.of(Map.of(
                                "check", "CALENDAR_ANCHOR",
                                "message", "semester_start_date is required to resolve fortnightly weeks"))));
    }

    /**
     * Resolves the campus for a department. Mirrors the A4-11 contract; the
     * department-to-campus wiring is a shared master-data integration point that
     * is not yet connected (see SchedulingDataLoader.getCampusIdForDepartment).
     */
    private Long resolveCampusId(Long departmentId) {
        // TODO: Resolve via DepartmentRepository when master-data wiring lands (shared with A4-11).
        return 1L;
    }

    private Map<String, Object> snapshot(ScheduledSession s) {
        Map<String, Object> m = new HashMap<>();
        m.put("recurrenceType", s.getRecurrenceType() != null ? s.getRecurrenceType().name() : null);
        m.put("weekGroup", s.getWeekGroup() != null ? s.getWeekGroup().name() : null);
        return m;
    }

    private SessionRecurrenceDto toDto(ScheduledSession s) {
        return SessionRecurrenceDto.builder()
                .sessionId(s.getId())
                .dayOfWeek(s.getDayOfWeek())
                .slotDefinitionId(s.getSlotDefinitionId())
                .recurrenceType(s.getRecurrenceType().name())
                .weekGroup(s.getWeekGroup() != null ? s.getWeekGroup().name() : null)
                .build();
    }
}
