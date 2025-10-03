package com.thanh.auction_server.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NoAccentsOrSpacesValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface NoAccentsOrSpaces {
    String message() default "Username must not contain accents or spaces";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

