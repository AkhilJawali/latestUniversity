package com.utms.masterdata.block;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class CreateBlockRequest {

    @NotBlank(message = "Resource type is required")
    @Pattern(regexp = "^(ROOM|ASSET)$", message = "Resource type must be ROOM or ASSET")
    private String resourceType;

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotBlank(message = "Block type is required")
    @Pattern(regexp = "^(HARD|SOFT)$", message = "Block type must be HARD or SOFT")
    private String blockType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotBlank(message = "Reason code is required")
    @Size(max = 50, message = "Reason code must not exceed 50 characters")
    private String reasonCode;

    @Size(max = 500, message = "Reason note must not exceed 500 characters")
    private String reasonNote;

    @Size(max = 100, message = "Recurrence pattern must not exceed 100 characters")
    private String recurrencePattern;
}
