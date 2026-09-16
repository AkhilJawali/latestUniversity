package com.utms.masterdata.academiccalendar;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class AcademicCalendarDto {
    private Long id;
    private Long campusId;
    private String academicYear;
    private String semesterIdentifier;
    private LocalDate semesterStartDate;
    private LocalDate semesterEndDate;
    private List<CalendarHolidayDto> holidays;
    private List<CalendarExamWindowDto> examWindows;
    private List<CalendarOrientationPeriodDto> orientationPeriods;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
