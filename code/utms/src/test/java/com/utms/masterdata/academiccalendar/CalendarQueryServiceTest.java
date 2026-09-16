package com.utms.masterdata.academiccalendar;

import com.utms.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarQueryServiceTest {

    @Mock
    private WorkingDayPatternRepository patternRepository;

    @Mock
    private AcademicCalendarRepository calendarRepository;

    @Mock
    private CalendarHolidayRepository holidayRepository;

    @Mock
    private CalendarExamWindowRepository examWindowRepository;

    @Mock
    private CalendarOrientationPeriodRepository orientationRepository;

    @InjectMocks
    private CalendarQueryService queryService;

    // --- isWorkingDay: no pattern configured (KD-39 fail-loudly) ---

    @Test
    void isWorkingDay_noPatternConfigured_throwsBusinessRule_KD39() {
        when(patternRepository.findByCampusIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> queryService.isWorkingDay(1L, LocalDate.of(2025, 9, 15)));

        assertTrue(exception.getMessage().contains("No working day pattern"));
        assertTrue(exception.getMessage().contains("KD-39"));
    }

    // --- isWorkingDay: holiday makes date non-working ---

    @Test
    void isWorkingDay_dateIsHoliday_returnsFalse() {
        // Monday (normally working day in FIVE_DAY pattern)
        LocalDate monday = LocalDate.of(2025, 8, 18);

        WorkingDayPattern pattern = new WorkingDayPattern();
        pattern.setPatternType(PatternType.FIVE_DAY);
        pattern.setCampusId(1L);

        AcademicCalendar calendar = new AcademicCalendar();
        calendar.setId(10L);

        CalendarHoliday holiday = new CalendarHoliday();
        holiday.setStartDate(monday);
        holiday.setEndDate(monday);

        when(patternRepository.findByCampusIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pattern));
        when(calendarRepository.findByCampusIdAndDeletedAtIsNull(1L)).thenReturn(List.of(calendar));
        when(holidayRepository.findByCalendarIdAndDateWithin(10L, monday)).thenReturn(List.of(holiday));

        boolean result = queryService.isWorkingDay(1L, monday);

        assertFalse(result);
    }

    // --- isWorkingDay: exam window makes date non-working ---

    @Test
    void isWorkingDay_dateInExamWindow_returnsFalse() {
        LocalDate examDay = LocalDate.of(2025, 10, 6); // Monday

        WorkingDayPattern pattern = new WorkingDayPattern();
        pattern.setPatternType(PatternType.FIVE_DAY);
        pattern.setCampusId(1L);

        AcademicCalendar calendar = new AcademicCalendar();
        calendar.setId(10L);

        CalendarExamWindow examWindow = new CalendarExamWindow();
        examWindow.setStartDate(examDay);
        examWindow.setEndDate(examDay.plusDays(5));

        when(patternRepository.findByCampusIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pattern));
        when(calendarRepository.findByCampusIdAndDeletedAtIsNull(1L)).thenReturn(List.of(calendar));
        when(holidayRepository.findByCalendarIdAndDateWithin(10L, examDay)).thenReturn(List.of());
        when(examWindowRepository.findByCalendarIdAndDateWithin(10L, examDay)).thenReturn(List.of(examWindow));

        boolean result = queryService.isWorkingDay(1L, examDay);

        assertFalse(result);
    }

    // --- isWorkingDay: orientation period makes date non-working ---

    @Test
    void isWorkingDay_dateInOrientationPeriod_returnsFalse() {
        LocalDate orientDay = LocalDate.of(2025, 7, 2); // Wednesday

        WorkingDayPattern pattern = new WorkingDayPattern();
        pattern.setPatternType(PatternType.FIVE_DAY);
        pattern.setCampusId(1L);

        AcademicCalendar calendar = new AcademicCalendar();
        calendar.setId(10L);

        CalendarOrientationPeriod orientation = new CalendarOrientationPeriod();
        orientation.setStartDate(LocalDate.of(2025, 7, 1));
        orientation.setEndDate(LocalDate.of(2025, 7, 5));

        when(patternRepository.findByCampusIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pattern));
        when(calendarRepository.findByCampusIdAndDeletedAtIsNull(1L)).thenReturn(List.of(calendar));
        when(holidayRepository.findByCalendarIdAndDateWithin(10L, orientDay)).thenReturn(List.of());
        when(examWindowRepository.findByCalendarIdAndDateWithin(10L, orientDay)).thenReturn(List.of());
        when(orientationRepository.findByCalendarIdAndDateWithin(10L, orientDay)).thenReturn(List.of(orientation));

        boolean result = queryService.isWorkingDay(1L, orientDay);

        assertFalse(result);
    }

    // --- isWorkingDay: FIVE_DAY pattern — weekday is working, Saturday/Sunday not ---

    @Test
    void isWorkingDay_fiveDayPattern_weekdayWithNoBlockers_returnsTrue() {
        LocalDate wednesday = LocalDate.of(2025, 9, 17); // Wednesday

        WorkingDayPattern pattern = new WorkingDayPattern();
        pattern.setPatternType(PatternType.FIVE_DAY);
        pattern.setCampusId(1L);

        when(patternRepository.findByCampusIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pattern));
        when(calendarRepository.findByCampusIdAndDeletedAtIsNull(1L)).thenReturn(List.of());

        boolean result = queryService.isWorkingDay(1L, wednesday);

        assertTrue(result);
    }

    @Test
    void isWorkingDay_fiveDayPattern_saturdayIsNotWorking() {
        LocalDate saturday = LocalDate.of(2025, 9, 20); // Saturday

        WorkingDayPattern pattern = new WorkingDayPattern();
        pattern.setPatternType(PatternType.FIVE_DAY);
        pattern.setCampusId(1L);

        when(patternRepository.findByCampusIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pattern));

        boolean result = queryService.isWorkingDay(1L, saturday);

        assertFalse(result);
    }

    // --- isWorkingDay: SIX_DAY pattern — Saturday is working, Sunday is not ---

    @Test
    void isWorkingDay_sixDayPattern_saturdayIsWorking() {
        LocalDate saturday = LocalDate.of(2025, 9, 20); // Saturday

        WorkingDayPattern pattern = new WorkingDayPattern();
        pattern.setPatternType(PatternType.SIX_DAY);
        pattern.setCampusId(1L);

        when(patternRepository.findByCampusIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pattern));
        when(calendarRepository.findByCampusIdAndDeletedAtIsNull(1L)).thenReturn(List.of());

        boolean result = queryService.isWorkingDay(1L, saturday);

        assertTrue(result);
    }
}
