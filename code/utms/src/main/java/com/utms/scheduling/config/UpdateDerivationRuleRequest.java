package com.utms.scheduling.config;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Update request. {@code campusId} is intentionally omitted — a config record's
 * campus is immutable (PD-84); changing campus means delete + recreate.
 */
@Getter
@Setter
@Builder
public class UpdateDerivationRuleRequest {

    @NotBlank(message = "Component type is required")
    @Size(max = 10, message = "Component type must not exceed 10 characters")
    @Pattern(regexp = "^(LECTURE|TUTORIAL|PRACTICAL)$",
            message = "Component type must be one of LECTURE, TUTORIAL, PRACTICAL")
    private String componentType;

    @NotNull(message = "Slot duration minutes is required")
    @Positive(message = "Slot duration minutes must be positive")
    private Integer slotDurationMinutes;

    @NotNull(message = "Hours per session is required")
    @DecimalMin(value = "0.0", message = "Hours per session must not be negative")
    @Digits(integer = 2, fraction = 1, message = "Hours per session must have at most one decimal place")
    private BigDecimal hoursPerSession;

    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    /** Active flag. Nullable: when present it is applied, letting the admin toggle the rule off. */
    private Boolean isActive;
}
