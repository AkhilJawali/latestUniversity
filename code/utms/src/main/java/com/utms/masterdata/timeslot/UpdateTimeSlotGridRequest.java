package com.utms.masterdata.timeslot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UpdateTimeSlotGridRequest {

    @NotBlank(message = "Grid name is required")
    @Size(max = 200, message = "Grid name must not exceed 200 characters")
    private String gridName;
}
