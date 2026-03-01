package com.user_service.validation.annotation;

import com.user_service.validation.validator.StrongPasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Password must contain uppercase, lowercase, digit and special character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

