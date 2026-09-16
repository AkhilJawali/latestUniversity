package com.utms.masterdata.faculty.availability;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Builder
public class CreateAvailabilityWindowRequest {

    @NotNull(message = "Day of week is required")
    private String dayOfWeek;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotBlank(message = "Reason code is required")
    @Size(max = 50, message = "Reason code must not exceed 50 characters")
    private String reasonCode;

    @Size(max = 500, message = "Reason note must not exceed 500 characters")
    private String reasonNote;
}
