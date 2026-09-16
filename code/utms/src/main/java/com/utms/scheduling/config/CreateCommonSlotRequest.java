package com.utms.scheduling.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateCommonSlotRequest {

    @NotNull(message = "Campus ID is required")
    private Long campusId;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Day of week is required")
    @Size(max = 10, message = "Day of week must not exceed 10 characters")
    @Pattern(regexp = "^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY)$",
            message = "Day of week must be one of MONDAY..SATURDAY")
    private String dayOfWeek;

    @NotNull(message = "Slot definition ID is required")
    private Long slotDefinitionId;

    private Boolean appliesToAllBatches;
}
