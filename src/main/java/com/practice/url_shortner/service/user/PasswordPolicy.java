package com.practice.url_shortner.service.user;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.practice.url_shortner.configuration.PasswordPolicyProperties;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Checks a raw, not yet encoded password against the configured {@link PasswordPolicyProperties}. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordPolicy {

	private final PasswordPolicyProperties properties;

	/** @throws CustomException with {@link ErrorCode#WEAK_PASSWORD} if the password breaks any rule */
	public void validate(String password) {
		if (password == null || password.length() < properties.minLength()
				|| password.getBytes(StandardCharsets.UTF_8).length > properties.maxLength()
				|| (properties.requireUppercase() && password.chars().noneMatch(Character::isUpperCase))
				|| (properties.requireLowercase() && password.chars().noneMatch(Character::isLowerCase))
				|| (properties.requireDigit() && password.chars().noneMatch(Character::isDigit))
				|| (properties.requireSpecial() && password.chars().allMatch(Character::isLetterOrDigit))) {
			log.info("Password rejected by the password policy.");
			throw CustomException.builder().errorCode(ErrorCode.WEAK_PASSWORD).build();
		}
	}
}
