package com.practice.url_shortner.validator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = SafeUrlValidator.class)
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
public @interface SafeUrl {

	String message() default "Invalid or unsafe URL.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
