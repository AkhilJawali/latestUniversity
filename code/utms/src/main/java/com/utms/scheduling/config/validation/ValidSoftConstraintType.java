package com.utms.scheduling.config.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates that a String is the name of a valid {@code SoftConstraintType} enum value.
 * Fails at the controller (400) so an invalid value can never be persisted — which would
 * otherwise break the scheduling engine's {@code SoftConstraintType.valueOf(...)} read (C-3).
 */
@Documented
@Constraint(validatedBy = SoftConstraintTypeValidator.class)
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface ValidSoftConstraintType {
    String message() default "must be a valid soft constraint type";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
