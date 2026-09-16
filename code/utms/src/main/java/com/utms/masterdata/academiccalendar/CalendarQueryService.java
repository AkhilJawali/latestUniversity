package com.utms.masterdata.academiccalendar;

import com.utms.common.exception.BusinessRuleViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read contract for the scheduling engine and conflict detection (FR-6.2).
 * Answers: "Is this date a working day for campus X?"
 * Checks: working-day pattern, holidays, exam windows, orientation periods.
 * Fails loudly if no pattern exists (KD-39).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarQueryService {

    private final WorkingDayPatternRepository patternRepository;
    private final AcademicCalendarRepository calendarRepository;
    private final CalendarHolidayRepository holidayRepository;
    private final CalendarExamWindowRepository examWindowRepository;
    private final CalendarOrientationPeriodRepository orientationRepository;

    /**
     * FR-6.2: "Is this date a working day for campus X?"
     * A date is a working day if ALL of the following are true:
     * 1. The day is a working day per the campus's working-day pattern (HC-CAL-2)
     * 2. The date is NOT a holiday (HC-CAL-1)
     * 3. The date is NOT within an exam window (HC-CAL-3)
     * 4. The date is NOT within an orientation period (HC-CAL-4)
     *
     * Fails with 422 if no working-day pattern exists for the campus (KD-39).
     */
    @Transactional(readOnly = true)
    public boolean isWorkingDay(Long campusId, LocalDate date) {
        // Step 1: Check working-day pattern — fail loudly if none exists
        WorkingDayPattern pattern = patternRepository.findByCampusIdAndDeletedAtIsNull(campusId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "No working day pattern configured for campus " + campusId + " (KD-39: pattern is required)",
                        List.of(Map.of("rule", "KD-39", "campusId", campusId.toString()))));

        if (!isDayWorkingPerPattern(date, pattern)) {
            return false;
        }

        // Step 2: Check holidays across all calendars for this campus
        List<AcademicCalendar> calendars = calendarRepository.findByCampusIdAndDeletedAtIsNull(campusId);
        for (AcademicCalendar calendar : calendars) {
            if (!holidayRepository.findByCalendarIdAndDateWithin(calendar.getId(), date).isEmpty()) {
                return false; // HC-CAL-1: date is a holiday
            }
        }

        // Step 3: Check exam windows
        for (AcademicCalendar calendar : calendars) {
            if (!examWindowRepository.findByCalendarIdAndDateWithin(calendar.getId(), date).isEmpty()) {
                return false; // HC-CAL-3: date is within exam window
            }
        }

        // Step 4: Check orientation periods
        for (AcademicCalendar calendar : calendars) {
            if (!orientationRepository.findByCalendarIdAndDateWithin(calendar.getId(), date).isEmpty()) {
                return false; // HC-CAL-4: date is within orientation period
            }
        }

        return true;
    }

    /**
     * Checks whether the date is within an exam window for the campus (HC-CAL-3).
     * Used by the exam scheduling module to determine valid exam placement dates.
     */
    @Transactional(readOnly = true)
    public boolean isExamWindow(Long campusId, LocalDate date) {
        List<AcademicCalendar> calendars = calendarRepository.findByCampusIdAndDeletedAtIsNull(campusId);
        for (AcademicCalendar calendar : calendars) {
            if (!examWindowRepository.findByCalendarIdAndDateWithin(calendar.getId(), date).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether a specific date is a working day per the campus's pattern.
     * Does NOT check holidays/exam windows — only the structural pattern.
     */
    private boolean isDayWorkingPerPattern(LocalDate date, WorkingDayPattern pattern) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        switch (pattern.getPatternType()) {
            case FIVE_DAY:
                // Mon-Fri working
                return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;

            case SIX_DAY:
                // Mon-Sat working
                return dayOfWeek != DayOfWeek.SUNDAY;

            case ALTERNATE_SATURDAY:
                // Mon-Fri always working, Sunday never, Saturday depends on ordinal
                if (dayOfWeek == DayOfWeek.SUNDAY) return false;
                if (dayOfWeek != DayOfWeek.SATURDAY) return true;
                // Determine which Saturday of the month this is (1st, 2nd, 3rd, etc.)
                int saturdayOrdinal = (date.getDayOfMonth() - 1) / 7 + 1;
                Set<Integer> workingSats = parseWorkingSaturdays(pattern.getWorkingSaturdays());
                return workingSats.contains(saturdayOrdinal);

            case CUSTOM:
                // Custom patterns: for now, default to 5-day unless parsing is implemented
                // TODO: Implement custom pattern parsing when requirements are clarified
                log.warn("CUSTOM pattern evaluation not fully implemented for campus {}; defaulting to 5-day", pattern.getCampusId());
                return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;

            default:
                return true;
        }
    }

    /**
     * Parses "1,3" into Set{1, 3} representing which Saturdays of the month are working.
     */
    private Set<Integer> parseWorkingSaturdays(String workingSaturdays) {
        if (workingSaturdays == null || workingSaturdays.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(workingSaturdays.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }
}
