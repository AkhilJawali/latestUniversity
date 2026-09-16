package com.utms.scheduling.engine.dto;

import com.utms.scheduling.engine.enums.RecurrenceType;
import com.utms.scheduling.engine.enums.WeekGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request to set or change a session's recurrence pattern (A4-13, design section 3).
 *
 * <p>{@code weekGroup} must be provided when {@code recurrenceType} is FORTNIGHTLY and
 * must be null when WEEKLY (HC-FN-4). This cross-field rule is enforced in the service
 * layer and by a DB CHECK constraint, since it cannot be expressed with a single-field
 * annotation.</p>
 */
@Getter
@Setter
@Schema(description = "Request to set or change a session's recurrence pattern")
public class UpdateRecurrenceRequest {

    @NotNull(message = "recurrenceType is required")
    @Schema(description = "Recurrence type", example = "FORTNIGHTLY", requiredMode = Schema.RequiredMode.REQUIRED)
    private RecurrenceType recurrenceType;

    @Schema(description = "Required when recurrenceType is FORTNIGHTLY; must be omitted when WEEKLY (HC-FN-4)",
            example = "WEEK_A")
    private WeekGroup weekGroup;
}
