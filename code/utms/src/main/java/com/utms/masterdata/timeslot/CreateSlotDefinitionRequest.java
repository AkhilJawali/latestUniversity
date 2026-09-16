package com.utms.masterdata.timeslot;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Builder
public class CreateSlotDefinitionRequest {

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Slot type is required")
    private SlotType slotType;

    /** Null means the slot applies to ALL days; non-null is a day-specific override. */
    private DayOfWeekEnum applicableDay;

    @Size(max = 100, message = "Label must not exceed 100 characters")
    private String label;
}
