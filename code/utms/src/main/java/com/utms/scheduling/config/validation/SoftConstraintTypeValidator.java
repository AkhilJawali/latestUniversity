package com.utms.scheduling.config.validation;

import com.utms.scheduling.engine.enums.SoftConstraintType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SoftConstraintTypeValidator
        implements ConstraintValidator<ValidSoftConstraintType, String> {

    private static final Set<String> ALLOWED = Arrays.stream(SoftConstraintType.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null / blank handled by @NotBlank; treat null as valid here to avoid duplicate messages.
        if (value == null) {
            return true;
        }
        return ALLOWED.contains(value);
    }
}
