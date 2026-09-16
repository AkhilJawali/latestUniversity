package com.utms.masterdata.faculty.availability;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SetPreferenceRequest {

    @NotBlank(message = "Preferred time of day is required")
    @Size(max = 20, message = "Preferred time of day must not exceed 20 characters")
    private String preferredTimeOfDay;

    @NotBlank(message = "Session distribution is required")
    @Size(max = 20, message = "Session distribution must not exceed 20 characters")
    private String sessionDistribution;
}
