package com.utms.masterdata.timeslot;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Builder
public class SlotDefinitionDto {
    private Long id;
    private Long gridId;
    private LocalTime startTime;
    private LocalTime endTime;
    private int durationMinutes;
    private SlotType slotType;
    private DayOfWeekEnum applicableDay;
    private String label;
}
