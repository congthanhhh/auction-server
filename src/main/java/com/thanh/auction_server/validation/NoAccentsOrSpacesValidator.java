package com.thanh.auction_server.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoAccentsOrSpacesValidator implements ConstraintValidator<NoAccentsOrSpaces, String> {
    private static final String REGEX = "^[a-zA-Z0-9_]+$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && value.matches(REGEX);
    }

    @Override
    public void initialize(NoAccentsOrSpaces constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }
}
