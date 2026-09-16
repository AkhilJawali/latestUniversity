package com.utms.masterdata.academiccalendar;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/academic-calendars")
@RequiredArgsConstructor
@Tag(name = "Academic Calendars", description = "Academic calendar management — semesters, holidays, exam windows, orientation periods")
public class AcademicCalendarController {

    private final AcademicCalendarService calendarService;
    private final WorkingDayPatternService patternService;
    private final CalendarQueryService queryService;

    // --- Calendar CRUD ---

    @PostMapping
    @Operation(summary = "Create an academic calendar for a campus/semester")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateAcademicCalendarRequest request) {
        AcademicCalendarDto calendar = calendarService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", calendar));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get academic calendar by ID (includes holidays, exam windows, orientation periods)")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        AcademicCalendarDto calendar = calendarService.findById(id);
        return ResponseEntity.ok(Map.of("data", calendar));
    }

    @GetMapping("/campus/{campusId}")
    @Operation(summary = "Get all academic calendars for a campus")
    public ResponseEntity<Map<String, Object>> getByCampus(@PathVariable Long campusId) {
        List<AcademicCalendarDto> calendars = calendarService.findByCampusId(campusId);
        return ResponseEntity.ok(Map.of("data", calendars));
    }

    @GetMapping("/year/{academicYear}")
    @Operation(summary = "Get all academic calendars for an academic year")
    public ResponseEntity<Map<String, Object>> getByYear(@PathVariable String academicYear) {
        List<AcademicCalendarDto> calendars = calendarService.findByAcademicYear(academicYear);
        return ResponseEntity.ok(Map.of("data", calendars));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an academic calendar and all its entries")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        calendarService.deleteCalendar(id);
        return ResponseEntity.noContent().build();
    }

    // --- Holiday management ---

    @PostMapping("/{calendarId}/holidays")
    @Operation(summary = "Add a holiday to a calendar (triggers impact detection if sessions exist)")
    public ResponseEntity<Map<String, Object>> addHoliday(
            @PathVariable Long calendarId,
            @Valid @RequestBody CreateHolidayRequest request) {
        CalendarHolidayDto holiday = calendarService.addHoliday(calendarId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", holiday));
    }

    @DeleteMapping("/{calendarId}/holidays/{holidayId}")
    @Operation(summary = "Remove a holiday from a calendar")
    public ResponseEntity<Void> removeHoliday(@PathVariable Long calendarId, @PathVariable Long holidayId) {
        calendarService.removeHoliday(calendarId, holidayId);
        return ResponseEntity.noContent().build();
    }

    // --- Exam window management ---

    @PostMapping("/{calendarId}/exam-windows")
    @Operation(summary = "Add an exam window to a calendar")
    public ResponseEntity<Map<String, Object>> addExamWindow(
            @PathVariable Long calendarId,
            @Valid @RequestBody CreateExamWindowRequest request) {
        CalendarExamWindowDto examWindow = calendarService.addExamWindow(calendarId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", examWindow));
    }

    // --- Orientation period management ---

    @PostMapping("/{calendarId}/orientation-periods")
    @Operation(summary = "Add an orientation/induction period to a calendar")
    public ResponseEntity<Map<String, Object>> addOrientationPeriod(
            @PathVariable Long calendarId,
            @Valid @RequestBody CreateOrientationPeriodRequest request) {
        CalendarOrientationPeriodDto period = calendarService.addOrientationPeriod(calendarId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", period));
    }

    // --- Working day pattern ---

    @PostMapping("/patterns")
    @Operation(summary = "Create a working day pattern for a campus (one per campus, required)")
    public ResponseEntity<Map<String, Object>> createPattern(@Valid @RequestBody CreateWorkingDayPatternRequest request) {
        WorkingDayPatternDto pattern = patternService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", pattern));
    }

    @GetMapping("/patterns/campus/{campusId}")
    @Operation(summary = "Get the working day pattern for a campus")
    public ResponseEntity<Map<String, Object>> getPattern(@PathVariable Long campusId) {
        WorkingDayPatternDto pattern = patternService.findByCampusId(campusId);
        return ResponseEntity.ok(Map.of("data", pattern));
    }

    @PutMapping("/patterns/campus/{campusId}")
    @Operation(summary = "Update working day pattern for a campus (triggers impact detection)")
    public ResponseEntity<Map<String, Object>> updatePattern(
            @PathVariable Long campusId,
            @Valid @RequestBody CreateWorkingDayPatternRequest request) {
        WorkingDayPatternDto pattern = patternService.update(campusId, request);
        return ResponseEntity.ok(Map.of("data", pattern));
    }

    // --- Calendar query (scheduling engine read contract) ---

    @GetMapping("/query/is-working-day")
    @Operation(summary = "Check if a date is a working day for a campus (FR-6.2)")
    public ResponseEntity<Map<String, Object>> isWorkingDay(
            @RequestParam Long campusId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean working = queryService.isWorkingDay(campusId, date);
        return ResponseEntity.ok(Map.of("data", Map.of("campusId", campusId, "date", date, "isWorkingDay", working)));
    }

    @GetMapping("/query/is-exam-window")
    @Operation(summary = "Check if a date falls within an exam window for a campus")
    public ResponseEntity<Map<String, Object>> isExamWindow(
            @RequestParam Long campusId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean examWindow = queryService.isExamWindow(campusId, date);
        return ResponseEntity.ok(Map.of("data", Map.of("campusId", campusId, "date", date, "isExamWindow", examWindow)));
    }
}
