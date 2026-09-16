package com.utms.masterdata.section;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UpdateSectionRequest {

    @Size(max = 10, message = "Section identifier must not exceed 10 characters")
    private String sectionIdentifier;

    @Positive(message = "Sub-strength must be a positive number")
    private Integer subStrength;
}
