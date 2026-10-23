package com.practice.url_shortner.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Rules a new password must satisfy: at registration, on password change and on
 * password reset.
 *
 * @param minLength        minimum length, in characters
 * @param maxLength        maximum length, in UTF-8 bytes; bcrypt only reads the
 *                         first 72 bytes of a password
 * @param requireUppercase at least one upper-case letter
 * @param requireLowercase at least one lower-case letter
 * @param requireDigit     at least one digit
 * @param requireSpecial   at least one character that is neither a letter nor a digit
 */
@ConfigurationProperties(prefix = "app.password-policy")
public record PasswordPolicyProperties(
		@DefaultValue("8") int minLength,
		@DefaultValue("72") int maxLength,
		@DefaultValue("false") boolean requireUppercase,
		@DefaultValue("false") boolean requireLowercase,
		@DefaultValue("false") boolean requireDigit,
		@DefaultValue("false") boolean requireSpecial) {

	public PasswordPolicyProperties {
		if (minLength < 1) {
			throw new IllegalArgumentException("app.password-policy.min-length must be at least 1");
		}
		if (maxLength < minLength || maxLength > 72) {
			throw new IllegalArgumentException(
					"app.password-policy.max-length must be between min-length and 72 (the bcrypt limit)");
		}
	}
}
