package com.utms.masterdata.academiccalendar;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateWorkingDayPatternRequest {

    @NotNull(message = "Campus ID is required")
    private Long campusId;

    @NotNull(message = "Pattern type is required")
    private PatternType patternType;

    /**
     * Required for ALTERNATE_SATURDAY: comma-separated ordinals, e.g., "1,3".
     */
    @Size(max = 100, message = "Working saturdays must not exceed 100 characters")
    private String workingSaturdays;

    /**
     * Required for CUSTOM pattern type.
     */
    private String customDefinition;
}
