package com.utms.scheduling.config;

import com.utms.scheduling.config.validation.ValidSoftConstraintType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Update request. {@code campusId} is intentionally omitted — campus is immutable (PD-84).
 */
@Getter
@Setter
@Builder
public class UpdateSoftConstraintWeightRequest {

    @NotBlank(message = "Constraint type is required")
    @ValidSoftConstraintType
    private String constraintType;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.00", message = "Weight must not be negative")
    @DecimalMax(value = "10.00", message = "Weight must not exceed 10.00")
    @Digits(integer = 2, fraction = 2, message = "Weight must have at most two decimal places")
    private BigDecimal weight;
}
