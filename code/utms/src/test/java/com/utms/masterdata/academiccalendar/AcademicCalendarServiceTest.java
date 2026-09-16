package com.utms.masterdata.academiccalendar;

import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicCalendarServiceTest {

    @Mock
    private AcademicCalendarRepository calendarRepository;

    @Mock
    private CalendarHolidayRepository holidayRepository;

    @Mock
    private CalendarExamWindowRepository examWindowRepository;

    @Mock
    private CalendarOrientationPeriodRepository orientationRepository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private AcademicCalendarMapper mapper;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AcademicCalendarService calendarService;

    // --- create calendar ---

    @Test
    void create_validRequest_returnsCalendarDto() {
        CreateAcademicCalendarRequest request = CreateAcademicCalendarRequest.builder()
                .campusId(1L)
                .academicYear("2025-26")
                .semesterIdentifier("ODD")
                .semesterStartDate(LocalDate.of(2025, 7, 1))
                .semesterEndDate(LocalDate.of(2025, 12, 15))
                .build();

        Campus campus = new Campus();
        campus.setId(1L);

        AcademicCalendar calendar = new AcademicCalendar();
        calendar.setId(1L);
        calendar.setCampusId(1L);
        calendar.setAcademicYear("2025-26");
        calendar.setSemesterIdentifier("ODD");
        calendar.setSemesterStartDate(LocalDate.of(2025, 7, 1));
        calendar.setSemesterEndDate(LocalDate.of(2025, 12, 15));

        AcademicCalendarDto expectedDto = AcademicCalendarDto.builder()
                .id(1L)
                .campusId(1L)
                .academicYear("2025-26")
                .semesterIdentifier("ODD")
                .build();

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(calendarRepository.existsByCampusIdAndAcademicYearAndSemesterIdentifierAndDeletedAtIsNull(
                1L, "2025-26", "ODD")).thenReturn(false);
        when(campusRepository.getReferenceById(1L)).thenReturn(campus);
        when(calendarRepository.save(any(AcademicCalendar.class))).thenReturn(calendar);
        when(mapper.toDto(any(AcademicCalendar.class))).thenReturn(expectedDto);

        AcademicCalendarDto result = calendarService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("2025-26", result.getAcademicYear());
        assertEquals("ODD", result.getSemesterIdentifier());
        verify(calendarRepository).save(any(AcademicCalendar.class));
    }

    @Test
    void create_duplicateCampusYearSemester_throwsConflict_KD40() {
        CreateAcademicCalendarRequest request = CreateAcademicCalendarRequest.builder()
                .campusId(1L)
                .academicYear("2025-26")
                .semesterIdentifier("ODD")
                .semesterStartDate(LocalDate.of(2025, 7, 1))
                .semesterEndDate(LocalDate.of(2025, 12, 15))
                .build();

        Campus campus = new Campus();
        campus.setId(1L);

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(calendarRepository.existsByCampusIdAndAcademicYearAndSemesterIdentifierAndDeletedAtIsNull(
                1L, "2025-26", "ODD")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> calendarService.create(request));

        assertTrue(exception.getMessage().contains("already exists"));
        verify(calendarRepository, never()).save(any());
    }

    @Test
    void create_startDateAfterEndDate_throwsBusinessRule() {
        CreateAcademicCalendarRequest request = CreateAcademicCalendarRequest.builder()
                .campusId(1L)
                .academicYear("2025-26")
                .semesterIdentifier("ODD")
                .semesterStartDate(LocalDate.of(2025, 12, 15))
                .semesterEndDate(LocalDate.of(2025, 7, 1))
                .build();

        Campus campus = new Campus();
        campus.setId(1L);

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> calendarService.create(request));

        assertTrue(exception.getMessage().contains("start date"));
        verify(calendarRepository, never()).save(any());
    }

    // --- addHoliday ---

    @Test
    void addHoliday_validRequest_publishesImpactEvent_KD38() {
        AcademicCalendar calendar = new AcademicCalendar();
        calendar.setId(1L);
        calendar.setCampusId(2L);
        calendar.setSemesterStartDate(LocalDate.of(2025, 7, 1));
        calendar.setSemesterEndDate(LocalDate.of(2025, 12, 15));

        CreateHolidayRequest request = CreateHolidayRequest.builder()
                .startDate(LocalDate.of(2025, 8, 15))
                .endDate(LocalDate.of(2025, 8, 15))
                .description("Independence Day")
                .scope(HolidayScope.INSTITUTION_WIDE)
                .build();

        CalendarHoliday holiday = new CalendarHoliday();
        holiday.setId(10L);
        holiday.setCalendarId(1L);
        holiday.setStartDate(LocalDate.of(2025, 8, 15));
        holiday.setEndDate(LocalDate.of(2025, 8, 15));
        holiday.setDescription("Independence Day");

        CalendarHolidayDto expectedDto = CalendarHolidayDto.builder()
                .id(10L)
                .calendarId(1L)
                .description("Independence Day")
                .scope(HolidayScope.INSTITUTION_WIDE)
                .build();

        when(calendarRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(calendar));
        when(mapper.toHolidayEntity(request)).thenReturn(holiday);
        when(holidayRepository.save(any(CalendarHoliday.class))).thenReturn(holiday);
        when(mapper.toHolidayDto(holiday)).thenReturn(expectedDto);

        CalendarHolidayDto result = calendarService.addHoliday(1L, request);

        assertNotNull(result);
        assertEquals("Independence Day", result.getDescription());
        assertEquals(HolidayScope.INSTITUTION_WIDE, result.getScope());
        // KD-38: verify impact event published for downstream conflict detection
        verify(eventPublisher).publishEvent(any(CalendarImpactEvent.class));
    }

    // --- addExamWindow ---

    @Test
    void addExamWindow_validRequest_savesSuccessfully() {
        AcademicCalendar calendar = new AcademicCalendar();
        calendar.setId(1L);
        calendar.setCampusId(2L);
        calendar.setSemesterStartDate(LocalDate.of(2025, 7, 1));
        calendar.setSemesterEndDate(LocalDate.of(2025, 12, 15));

        CreateExamWindowRequest request = CreateExamWindowRequest.builder()
                .startDate(LocalDate.of(2025, 9, 15))
                .endDate(LocalDate.of(2025, 9, 25))
                .examType(ExamType.MID_SEMESTER)
                .description("Mid-semester exams")
                .build();

        CalendarExamWindow examWindow = new CalendarExamWindow();
        examWindow.setId(5L);
        examWindow.setCalendarId(1L);

        CalendarExamWindowDto expectedDto = CalendarExamWindowDto.builder()
                .id(5L)
                .calendarId(1L)
                .examType(ExamType.MID_SEMESTER)
                .build();

        when(calendarRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(calendar));
        when(mapper.toExamWindowEntity(request)).thenReturn(examWindow);
        when(examWindowRepository.save(any(CalendarExamWindow.class))).thenReturn(examWindow);
        when(mapper.toExamWindowDto(examWindow)).thenReturn(expectedDto);

        CalendarExamWindowDto result = calendarService.addExamWindow(1L, request);

        assertNotNull(result);
        assertEquals(ExamType.MID_SEMESTER, result.getExamType());
        verify(examWindowRepository).save(any(CalendarExamWindow.class));
    }

    @Test
    void addExamWindow_outsideSemesterBounds_throwsBusinessRule() {
        AcademicCalendar calendar = new AcademicCalendar();
        calendar.setId(1L);
        calendar.setSemesterStartDate(LocalDate.of(2025, 7, 1));
        calendar.setSemesterEndDate(LocalDate.of(2025, 12, 15));

        CreateExamWindowRequest request = CreateExamWindowRequest.builder()
                .startDate(LocalDate.of(2026, 1, 5))
                .endDate(LocalDate.of(2026, 1, 15))
                .examType(ExamType.END_SEMESTER)
                .build();

        when(calendarRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(calendar));

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> calendarService.addExamWindow(1L, request));

        assertTrue(exception.getMessage().contains("within semester bounds"));
        verify(examWindowRepository, never()).save(any());
    }

    // --- addOrientationPeriod ---

    @Test
    void addOrientationPeriod_validRequest_savesSuccessfully() {
        AcademicCalendar calendar = new AcademicCalendar();
        calendar.setId(1L);
        calendar.setCampusId(2L);
        calendar.setSemesterStartDate(LocalDate.of(2025, 7, 1));
        calendar.setSemesterEndDate(LocalDate.of(2025, 12, 15));

        CreateOrientationPeriodRequest request = CreateOrientationPeriodRequest.builder()
                .startDate(LocalDate.of(2025, 7, 1))
                .endDate(LocalDate.of(2025, 7, 5))
                .description("Freshers Orientation")
                .build();

        CalendarOrientationPeriod period = new CalendarOrientationPeriod();
        period.setId(3L);
        period.setCalendarId(1L);

        CalendarOrientationPeriodDto expectedDto = CalendarOrientationPeriodDto.builder()
                .id(3L)
                .calendarId(1L)
                .description("Freshers Orientation")
                .build();

        when(calendarRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(calendar));
        when(mapper.toOrientationPeriodEntity(request)).thenReturn(period);
        when(orientationRepository.save(any(CalendarOrientationPeriod.class))).thenReturn(period);
        when(mapper.toOrientationPeriodDto(period)).thenReturn(expectedDto);

        CalendarOrientationPeriodDto result = calendarService.addOrientationPeriod(1L, request);

        assertNotNull(result);
        assertEquals("Freshers Orientation", result.getDescription());
        verify(orientationRepository).save(any(CalendarOrientationPeriod.class));
    }

    // --- deleteCalendar ---

    @Test
    void deleteCalendar_existing_softDeletesCalendarAndChildren() {
        AcademicCalendar calendar = new AcademicCalendar();
        calendar.setId(1L);
        calendar.setIsActive(true);

        when(calendarRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(calendar));
        when(holidayRepository.findByCalendarIdAndDeletedAtIsNull(1L)).thenReturn(List.of());
        when(examWindowRepository.findByCalendarIdAndDeletedAtIsNull(1L)).thenReturn(List.of());
        when(orientationRepository.findByCalendarIdAndDeletedAtIsNull(1L)).thenReturn(List.of());

        calendarService.deleteCalendar(1L);

        assertNotNull(calendar.getDeletedAt());
        assertFalse(calendar.getIsActive());
        verify(calendarRepository).save(calendar);
    }
}
