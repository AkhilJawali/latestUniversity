package com.utms.scheduling.engine.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionRecurrenceServiceTest {

    @Mock private ScheduledSessionRepository sessionRepository;
    @Mock private TimetableDraftRepository draftRepository;
    @Mock private AcademicCalendarRepository calendarRepository;
    @Mock private CalendarQueryService calendarQueryService;
    @Mock private AuditEventPublisher auditPublisher;

    // Real resolver — pure logic, no need to mock.
    private final WeekParityResolver parityResolver = new WeekParityResolver();

    private SessionRecurrenceService service;

    @BeforeEach
    void setUp() {
        service = new SessionRecurrenceService(
                sessionRepository, draftRepository, calendarRepository,
                calendarQueryService, parityResolver, auditPublisher);
    }

    private ScheduledSession session(Long id, String day, RecurrenceType type, WeekGroup group) {
        ScheduledSession s = new ScheduledSession();
        s.setId(id);
        s.setDraftId(10L);
        s.setDayOfWeek(day);
        s.setSlotDefinitionId(33L);
        s.setRecurrenceType(type);
        s.setWeekGroup(group);
        return s;
    }

    // --- setRecurrence validation (HC-FN-4) ---

    @Test
    void setRecurrence_weeklyWithWeekGroup_throwsBusinessRule() {
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(session(1L, "MONDAY", RecurrenceType.WEEKLY, null)));

        assertThrows(BusinessRuleViolationException.class,
                () -> service.setRecurrence(1L, RecurrenceType.WEEKLY, WeekGroup.WEEK_A, "tester"));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void setRecurrence_fortnightlyWithoutWeekGroup_throwsBusinessRule() {
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(session(1L, "MONDAY", RecurrenceType.WEEKLY, null)));

        assertThrows(BusinessRuleViolationException.class,
                () -> service.setRecurrence(1L, RecurrenceType.FORTNIGHTLY, null, "tester"));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void setRecurrence_sessionNotFound_throwsNotFound() {
        when(sessionRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> service.setRecurrence(99L, RecurrenceType.WEEKLY, null, "tester"));
    }

    @Test
    void setRecurrence_validFortnightly_persistsAndAudits() {
        ScheduledSession s = session(1L, "MONDAY", RecurrenceType.WEEKLY, null);
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(s));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SessionRecurrenceDto dto = service.setRecurrence(1L, RecurrenceType.FORTNIGHTLY, WeekGroup.WEEK_A, "tester");

        assertEquals("FORTNIGHTLY", dto.getRecurrenceType());
        assertEquals("WEEK_A", dto.getWeekGroup());
        verify(auditPublisher, times(1)).publish(any());
    }

    @Test
    void revertToWeekly_clearsWeekGroup() {
        ScheduledSession s = session(1L, "MONDAY", RecurrenceType.FORTNIGHTLY, WeekGroup.WEEK_A);
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(s));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SessionRecurrenceDto dto = service.revertToWeekly(1L, "tester");

        assertEquals("WEEKLY", dto.getRecurrenceType());
        assertNull(dto.getWeekGroup());
        assertNull(s.getWeekGroup());
    }

    @Test
    void setRecurrence_doesNotChangeDayOrSlot_FR15() {
        ScheduledSession s = session(1L, "MONDAY", RecurrenceType.WEEKLY, null);
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(s));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String dayBefore = s.getDayOfWeek();
        Long slotBefore = s.getSlotDefinitionId();

        SessionRecurrenceDto dto = service.setRecurrence(1L, RecurrenceType.FORTNIGHTLY, WeekGroup.WEEK_B, "tester");

        // FR-1.5 / KD-64: recurrence change never moves the session's day or slot.
        assertEquals(dayBefore, s.getDayOfWeek());
        assertEquals(slotBefore, s.getSlotDefinitionId());
        assertEquals(dayBefore, dto.getDayOfWeek());
        assertEquals(slotBefore, dto.getSlotDefinitionId());
    }

    @Test
    void revertToWeekly_doesNotChangeDayOrSlot_FR15() {
        ScheduledSession s = session(1L, "TUESDAY", RecurrenceType.FORTNIGHTLY, WeekGroup.WEEK_A);
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(s));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String dayBefore = s.getDayOfWeek();
        Long slotBefore = s.getSlotDefinitionId();

        service.revertToWeekly(1L, "tester");

        assertEquals(dayBefore, s.getDayOfWeek());
        assertEquals(slotBefore, s.getSlotDefinitionId());
    }

    // --- getOccurrenceDates ---

    private TimetableDraft draft() {
        TimetableDraft d = new TimetableDraft();
        d.setId(10L);
        d.setDepartmentId(5L);
        d.setAcademicYear("2025-26");
        d.setSemester("ODD");
        return d;
    }

    private AcademicCalendar calendar(LocalDate start, LocalDate end) {
        AcademicCalendar c = new AcademicCalendar();
        c.setSemesterStartDate(start);
        c.setSemesterEndDate(end);
        return c;
    }

    @Test
    void getOccurrenceDates_fortnightlyWeekA_returnsOnlyWeekAMondays() {
        // Semester: Mon 2026-01-05 .. Fri 2026-01-30 (4 Mondays: 05,12,19,26)
        ScheduledSession s = session(1L, "MONDAY", RecurrenceType.FORTNIGHTLY, WeekGroup.WEEK_A);
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(s));
        when(draftRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(draft()));
        when(calendarRepository.findByCampusIdAndAcademicYearAndSemesterIdentifierAndDeletedAtIsNull(
                anyLong(), eq("2025-26"), eq("ODD")))
                .thenReturn(Optional.of(calendar(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 30))));
        when(calendarQueryService.isWorkingDay(anyLong(), any())).thenReturn(true);

        OccurrenceDatesDto dto = service.getOccurrenceDates(1L);

        // WEEK_A Mondays only: 05 (wk0) and 19 (wk2)
        assertEquals(2, dto.getOccurrenceDates().size());
        assertTrue(dto.getOccurrenceDates().contains(LocalDate.of(2026, 1, 5)));
        assertTrue(dto.getOccurrenceDates().contains(LocalDate.of(2026, 1, 19)));
    }

    @Test
    void getOccurrenceDates_holidayOccurrence_isSkipped() {
        ScheduledSession s = session(1L, "MONDAY", RecurrenceType.FORTNIGHTLY, WeekGroup.WEEK_A);
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(s));
        when(draftRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(draft()));
        when(calendarRepository.findByCampusIdAndAcademicYearAndSemesterIdentifierAndDeletedAtIsNull(
                anyLong(), any(), any()))
                .thenReturn(Optional.of(calendar(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 30))));
        // 2026-01-05 is a holiday, 2026-01-19 is working
        when(calendarQueryService.isWorkingDay(anyLong(), eq(LocalDate.of(2026, 1, 5)))).thenReturn(false);
        when(calendarQueryService.isWorkingDay(anyLong(), eq(LocalDate.of(2026, 1, 19)))).thenReturn(true);

        OccurrenceDatesDto dto = service.getOccurrenceDates(1L);

        assertEquals(1, dto.getOccurrenceDates().size());
        assertTrue(dto.getOccurrenceDates().contains(LocalDate.of(2026, 1, 19)));
        assertFalse(dto.getOccurrenceDates().contains(LocalDate.of(2026, 1, 5)));
    }

    @Test
    void getOccurrenceDates_weekly_returnsAllWorkingOccurrences() {
        ScheduledSession s = session(1L, "MONDAY", RecurrenceType.WEEKLY, null);
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(s));
        when(draftRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(draft()));
        when(calendarRepository.findByCampusIdAndAcademicYearAndSemesterIdentifierAndDeletedAtIsNull(
                anyLong(), any(), any()))
                .thenReturn(Optional.of(calendar(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 30))));
        when(calendarQueryService.isWorkingDay(anyLong(), any())).thenReturn(true);

        OccurrenceDatesDto dto = service.getOccurrenceDates(1L);

        // All 4 Mondays in the range
        assertEquals(4, dto.getOccurrenceDates().size());
    }

    @Test
    void getOccurrenceDates_noCalendarAnchor_throwsBusinessRule() {
        ScheduledSession s = session(1L, "MONDAY", RecurrenceType.FORTNIGHTLY, WeekGroup.WEEK_A);
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(s));
        when(draftRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(draft()));
        when(calendarRepository.findByCampusIdAndAcademicYearAndSemesterIdentifierAndDeletedAtIsNull(
                anyLong(), any(), any())).thenReturn(Optional.empty());

        assertThrows(BusinessRuleViolationException.class, () -> service.getOccurrenceDates(1L));
    }

    @Test
    void getOccurrenceDates_invertedCalendar_throwsBusinessRule() {
        ScheduledSession s = session(1L, "MONDAY", RecurrenceType.FORTNIGHTLY, WeekGroup.WEEK_A);
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(s));
        when(draftRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(draft()));
        // Misconfigured calendar: end precedes start.
        when(calendarRepository.findByCampusIdAndAcademicYearAndSemesterIdentifierAndDeletedAtIsNull(
                anyLong(), any(), any()))
                .thenReturn(Optional.of(calendar(LocalDate.of(2026, 1, 30), LocalDate.of(2026, 1, 5))));

        assertThrows(BusinessRuleViolationException.class, () -> service.getOccurrenceDates(1L));
    }
}
