package com.practice.url_shortner.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.practice.url_shortner.constants.ErrorCode;

/**
 * Declares the endpoint-specific {@link ErrorCode}s a controller method can
 * return, so they show up as named examples in the OpenAPI document. Codes that
 * apply to every endpoint (rate limiting, validation, authentication) are added
 * automatically by {@code SwaggerConfig} and don't need to be listed.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApiErrors {

	ErrorCode[] value();
}
