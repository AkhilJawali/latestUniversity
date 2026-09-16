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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicCalendarService {

    private final AcademicCalendarRepository calendarRepository;
    private final CalendarHolidayRepository holidayRepository;
    private final CalendarExamWindowRepository examWindowRepository;
    private final CalendarOrientationPeriodRepository orientationRepository;
    private final CampusRepository campusRepository;
    private final AcademicCalendarMapper mapper;
    private final AuditEventPublisher auditEventPublisher;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AcademicCalendarDto create(CreateAcademicCalendarRequest request) {
        // Validate campus exists
        if (!campusRepository.findByIdAndDeletedAtIsNull(request.getCampusId()).isPresent()) {
            throw new EntityNotFoundException("Campus", request.getCampusId());
        }

        // HC-CAL-5: start <= end
        if (request.getSemesterStartDate().isAfter(request.getSemesterEndDate())) {
            throw new BusinessRuleViolationException(
                    "Semester start date must be on or before end date",
                    List.of(Map.of("rule", "HC-CAL-5",
                            "startDate", request.getSemesterStartDate().toString(),
                            "endDate", request.getSemesterEndDate().toString())));
        }

        // KD-40: unique per campus+year+semester
        if (calendarRepository.existsByCampusIdAndAcademicYearAndSemesterIdentifierAndDeletedAtIsNull(
                request.getCampusId(), request.getAcademicYear(), request.getSemesterIdentifier())) {
            throw new ConflictException(String.format(
                    "Calendar already exists for campus %d, year '%s', semester '%s'",
                    request.getCampusId(), request.getAcademicYear(), request.getSemesterIdentifier()));
        }

        // Persist calendar
        AcademicCalendar calendar = new AcademicCalendar();
        calendar.setCampus(campusRepository.getReferenceById(request.getCampusId()));
        calendar.setAcademicYear(request.getAcademicYear());
        calendar.setSemesterIdentifier(request.getSemesterIdentifier());
        calendar.setSemesterStartDate(request.getSemesterStartDate());
        calendar.setSemesterEndDate(request.getSemesterEndDate());
        calendar.setIsActive(true);
        calendar = calendarRepository.save(calendar);

        // Persist child entries
        List<CalendarHoliday> holidays = new ArrayList<>();
        if (request.getHolidays() != null) {
            for (CreateHolidayRequest h : request.getHolidays()) {
                validateDateRange(h.getStartDate(), h.getEndDate(), "Holiday");
                CalendarHoliday holiday = mapper.toHolidayEntity(h);
                holiday.setCalendar(calendar);
                holiday.setIsActive(true);
                holidays.add(holidayRepository.save(holiday));
            }
        }

        List<CalendarExamWindow> examWindows = new ArrayList<>();
        if (request.getExamWindows() != null) {
            for (CreateExamWindowRequest e : request.getExamWindows()) {
                validateDateRange(e.getStartDate(), e.getEndDate(), "Exam window");
                // HC-CAL-6: exam window must fall within semester bounds
                validateWithinSemester(e.getStartDate(), e.getEndDate(),
                        request.getSemesterStartDate(), request.getSemesterEndDate(), "Exam window");
                CalendarExamWindow examWindow = mapper.toExamWindowEntity(e);
                examWindow.setCalendar(calendar);
                examWindow.setIsActive(true);
                examWindows.add(examWindowRepository.save(examWindow));
            }
        }

        List<CalendarOrientationPeriod> orientations = new ArrayList<>();
        if (request.getOrientationPeriods() != null) {
            for (CreateOrientationPeriodRequest o : request.getOrientationPeriods()) {
                validateDateRange(o.getStartDate(), o.getEndDate(), "Orientation period");
                validateWithinSemester(o.getStartDate(), o.getEndDate(),
                        request.getSemesterStartDate(), request.getSemesterEndDate(), "Orientation period");
                CalendarOrientationPeriod period = mapper.toOrientationPeriodEntity(o);
                period.setCalendar(calendar);
                period.setIsActive(true);
                orientations.add(orientationRepository.save(period));
            }
        }

        calendar.setHolidays(holidays);
        calendar.setExamWindows(examWindows);
        calendar.setOrientationPeriods(orientations);

        auditEventPublisher.publish(new AuditEvent("AcademicCalendar", calendar.getId(),
                AuditEvent.Action.CREATED, null, calendar, "system", Instant.now()));

        log.info("Academic calendar created: id={}, campus={}, year={}, semester={}",
                calendar.getId(), request.getCampusId(), request.getAcademicYear(), request.getSemesterIdentifier());
        return mapper.toDto(calendar);
    }

    @Transactional(readOnly = true)
    public AcademicCalendarDto findById(Long id) {
        AcademicCalendar calendar = findActiveOrThrow(id);
        loadChildren(calendar);
        return mapper.toDto(calendar);
    }

    @Transactional(readOnly = true)
    public List<AcademicCalendarDto> findByCampusId(Long campusId) {
        List<AcademicCalendar> calendars = calendarRepository.findByCampusIdAndDeletedAtIsNull(campusId);
        calendars.forEach(this::loadChildren);
        return calendars.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AcademicCalendarDto> findByAcademicYear(String academicYear) {
        List<AcademicCalendar> calendars = calendarRepository.findByAcademicYearAndDeletedAtIsNull(academicYear);
        calendars.forEach(this::loadChildren);
        return calendars.stream().map(mapper::toDto).toList();
    }

    @Transactional
    public CalendarHolidayDto addHoliday(Long calendarId, CreateHolidayRequest request) {
        AcademicCalendar calendar = findActiveOrThrow(calendarId);
        validateDateRange(request.getStartDate(), request.getEndDate(), "Holiday");

        CalendarHoliday holiday = mapper.toHolidayEntity(request);
        holiday.setCalendar(calendar);
        holiday.setIsActive(true);
        holiday = holidayRepository.save(holiday);

        auditEventPublisher.publish(new AuditEvent("CalendarHoliday", holiday.getId(),
                AuditEvent.Action.CREATED, null, holiday, "system", Instant.now()));

        // FR-7.1: Emit impact detection event (KD-38) — consumed by conflict detection module
        eventPublisher.publishEvent(new CalendarImpactEvent(
                calendar.getCampusId(), request.getStartDate(), request.getEndDate(),
                "HOLIDAY_ADDED", holiday.getDescription()));

        log.info("Holiday added to calendar: calendarId={}, holidayId={}, dates=[{} to {}]",
                calendarId, holiday.getId(), request.getStartDate(), request.getEndDate());
        return mapper.toHolidayDto(holiday);
    }

    @Transactional
    public void removeHoliday(Long calendarId, Long holidayId) {
        findActiveOrThrow(calendarId);
        CalendarHoliday holiday = holidayRepository.findByIdAndDeletedAtIsNull(holidayId)
                .orElseThrow(() -> new EntityNotFoundException("CalendarHoliday", holidayId));

        if (!holiday.getCalendarId().equals(calendarId)) {
            throw new EntityNotFoundException("CalendarHoliday", holidayId);
        }

        holiday.setDeletedAt(LocalDateTime.now());
        holiday.setIsActive(false);
        holidayRepository.save(holiday);

        auditEventPublisher.publish(new AuditEvent("CalendarHoliday", holiday.getId(),
                AuditEvent.Action.DELETED, holiday, null, "system", Instant.now()));

        log.info("Holiday removed: calendarId={}, holidayId={}", calendarId, holidayId);
    }

    @Transactional
    public CalendarExamWindowDto addExamWindow(Long calendarId, CreateExamWindowRequest request) {
        AcademicCalendar calendar = findActiveOrThrow(calendarId);
        validateDateRange(request.getStartDate(), request.getEndDate(), "Exam window");
        // HC-CAL-6
        validateWithinSemester(request.getStartDate(), request.getEndDate(),
                calendar.getSemesterStartDate(), calendar.getSemesterEndDate(), "Exam window");

        CalendarExamWindow examWindow = mapper.toExamWindowEntity(request);
        examWindow.setCalendar(calendar);
        examWindow.setIsActive(true);
        examWindow = examWindowRepository.save(examWindow);

        auditEventPublisher.publish(new AuditEvent("CalendarExamWindow", examWindow.getId(),
                AuditEvent.Action.CREATED, null, examWindow, "system", Instant.now()));

        log.info("Exam window added: calendarId={}, examWindowId={}, type={}",
                calendarId, examWindow.getId(), request.getExamType());
        return mapper.toExamWindowDto(examWindow);
    }

    @Transactional
    public CalendarOrientationPeriodDto addOrientationPeriod(Long calendarId, CreateOrientationPeriodRequest request) {
        AcademicCalendar calendar = findActiveOrThrow(calendarId);
        validateDateRange(request.getStartDate(), request.getEndDate(), "Orientation period");
        validateWithinSemester(request.getStartDate(), request.getEndDate(),
                calendar.getSemesterStartDate(), calendar.getSemesterEndDate(), "Orientation period");

        CalendarOrientationPeriod period = mapper.toOrientationPeriodEntity(request);
        period.setCalendar(calendar);
        period.setIsActive(true);
        period = orientationRepository.save(period);

        auditEventPublisher.publish(new AuditEvent("CalendarOrientationPeriod", period.getId(),
                AuditEvent.Action.CREATED, null, period, "system", Instant.now()));

        log.info("Orientation period added: calendarId={}, periodId={}", calendarId, period.getId());
        return mapper.toOrientationPeriodDto(period);
    }

    @Transactional
    public void deleteCalendar(Long id) {
        AcademicCalendar calendar = findActiveOrThrow(id);

        // Soft-delete children
        holidayRepository.findByCalendarIdAndDeletedAtIsNull(id)
                .forEach(h -> { h.setDeletedAt(LocalDateTime.now()); h.setIsActive(false); holidayRepository.save(h); });
        examWindowRepository.findByCalendarIdAndDeletedAtIsNull(id)
                .forEach(e -> { e.setDeletedAt(LocalDateTime.now()); e.setIsActive(false); examWindowRepository.save(e); });
        orientationRepository.findByCalendarIdAndDeletedAtIsNull(id)
                .forEach(o -> { o.setDeletedAt(LocalDateTime.now()); o.setIsActive(false); orientationRepository.save(o); });

        calendar.setDeletedAt(LocalDateTime.now());
        calendar.setIsActive(false);
        calendarRepository.save(calendar);

        auditEventPublisher.publish(new AuditEvent("AcademicCalendar", calendar.getId(),
                AuditEvent.Action.DELETED, calendar, null, "system", Instant.now()));

        log.info("Academic calendar soft-deleted: id={}", id);
    }

    // --- Validation helpers ---

    private void validateDateRange(LocalDate start, LocalDate end, String label) {
        if (start.isAfter(end)) {
            throw new BusinessRuleViolationException(
                    label + " start date must be on or before end date",
                    List.of(Map.of("field", "startDate", "rejectedValue", start.toString(),
                            "endDate", end.toString())));
        }
    }

    private void validateWithinSemester(LocalDate start, LocalDate end,
                                        LocalDate semStart, LocalDate semEnd, String label) {
        if (start.isBefore(semStart) || end.isAfter(semEnd)) {
            throw new BusinessRuleViolationException(
                    label + " must fall within semester bounds [" + semStart + " to " + semEnd + "]",
                    List.of(Map.of("rule", "HC-CAL-6", "startDate", start.toString(),
                            "endDate", end.toString(), "semesterStart", semStart.toString(),
                            "semesterEnd", semEnd.toString())));
        }
    }

    private AcademicCalendar findActiveOrThrow(Long id) {
        return calendarRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("AcademicCalendar", id));
    }

    private void loadChildren(AcademicCalendar calendar) {
        calendar.setHolidays(holidayRepository.findByCalendarIdAndDeletedAtIsNull(calendar.getId()));
        calendar.setExamWindows(examWindowRepository.findByCalendarIdAndDeletedAtIsNull(calendar.getId()));
        calendar.setOrientationPeriods(orientationRepository.findByCalendarIdAndDeletedAtIsNull(calendar.getId()));
    }
}
