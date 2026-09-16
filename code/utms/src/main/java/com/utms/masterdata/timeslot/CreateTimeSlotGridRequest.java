package com.utms.masterdata.timeslot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CreateTimeSlotGridRequest {

    @NotNull(message = "Campus ID is required")
    private Long campusId;

    @NotBlank(message = "Grid name is required")
    @Size(max = 200, message = "Grid name must not exceed 200 characters")
    private String gridName;

    @NotNull(message = "At least one slot definition is required")
    @Size(min = 1, message = "At least one slot definition is required")
    @Valid
    private List<CreateSlotDefinitionRequest> slots;
}
