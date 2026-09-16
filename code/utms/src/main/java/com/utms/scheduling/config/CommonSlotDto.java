package com.utms.scheduling.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CommonSlotDto {
    private Long id;
    private Long campusId;
    private String name;
    private String dayOfWeek;
    private Long slotDefinitionId;
    private Boolean appliesToAllBatches;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
