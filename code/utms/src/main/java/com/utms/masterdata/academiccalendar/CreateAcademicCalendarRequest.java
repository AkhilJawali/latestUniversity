package com.utms.masterdata.academiccalendar;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class CreateAcademicCalendarRequest {

    @NotNull(message = "Campus ID is required")
    private Long campusId;

    @NotBlank(message = "Academic year is required")
    @Size(max = 20, message = "Academic year must not exceed 20 characters")
    private String academicYear;

    @NotBlank(message = "Semester identifier is required")
    @Size(max = 30, message = "Semester identifier must not exceed 30 characters")
    private String semesterIdentifier;

    @NotNull(message = "Semester start date is required")
    private LocalDate semesterStartDate;

    @NotNull(message = "Semester end date is required")
    private LocalDate semesterEndDate;

    @Valid
    private List<CreateHolidayRequest> holidays;

    @Valid
    private List<CreateExamWindowRequest> examWindows;

    @Valid
    private List<CreateOrientationPeriodRequest> orientationPeriods;
}
