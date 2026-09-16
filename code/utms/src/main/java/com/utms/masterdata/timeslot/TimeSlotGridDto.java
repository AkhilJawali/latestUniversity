package com.utms.masterdata.timeslot;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class TimeSlotGridDto {
    private Long id;
    private Long campusId;
    private String gridName;
    private List<SlotDefinitionDto> slots;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
