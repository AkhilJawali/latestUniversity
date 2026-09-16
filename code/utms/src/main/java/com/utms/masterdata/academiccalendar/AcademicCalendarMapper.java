package com.utms.masterdata.academiccalendar;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = BaseMapperConfig.class)
public interface AcademicCalendarMapper {

    @Mapping(target = "campusId", source = "campusId")
    @Mapping(target = "holidays", source = "holidays")
    @Mapping(target = "examWindows", source = "examWindows")
    @Mapping(target = "orientationPeriods", source = "orientationPeriods")
    AcademicCalendarDto toDto(AcademicCalendar calendar);

    @Mapping(target = "calendarId", source = "calendarId")
    CalendarHolidayDto toHolidayDto(CalendarHoliday holiday);

    @Mapping(target = "calendarId", source = "calendarId")
    CalendarExamWindowDto toExamWindowDto(CalendarExamWindow examWindow);

    @Mapping(target = "calendarId", source = "calendarId")
    CalendarOrientationPeriodDto toOrientationPeriodDto(CalendarOrientationPeriod period);

    @Mapping(target = "campusId", source = "campusId")
    WorkingDayPatternDto toPatternDto(WorkingDayPattern pattern);

    List<CalendarHolidayDto> toHolidayDtoList(List<CalendarHoliday> holidays);

    List<CalendarExamWindowDto> toExamWindowDtoList(List<CalendarExamWindow> examWindows);

    List<CalendarOrientationPeriodDto> toOrientationPeriodDtoList(List<CalendarOrientationPeriod> periods);

    @Mapping(target = "calendar", ignore = true)
    @Mapping(target = "calendarId", ignore = true)
    CalendarHoliday toHolidayEntity(CreateHolidayRequest request);

    @Mapping(target = "calendar", ignore = true)
    @Mapping(target = "calendarId", ignore = true)
    CalendarExamWindow toExamWindowEntity(CreateExamWindowRequest request);

    @Mapping(target = "calendar", ignore = true)
    @Mapping(target = "calendarId", ignore = true)
    CalendarOrientationPeriod toOrientationPeriodEntity(CreateOrientationPeriodRequest request);
}
