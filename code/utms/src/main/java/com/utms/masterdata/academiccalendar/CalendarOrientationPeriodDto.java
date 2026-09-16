package com.utms.masterdata.academiccalendar;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class CalendarOrientationPeriodDto {
    private Long id;
    private Long calendarId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
}
