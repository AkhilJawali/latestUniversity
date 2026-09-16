package com.utms.masterdata.academiccalendar;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class CalendarExamWindowDto {
    private Long id;
    private Long calendarId;
    private LocalDate startDate;
    private LocalDate endDate;
    private ExamType examType;
    private String description;
}
