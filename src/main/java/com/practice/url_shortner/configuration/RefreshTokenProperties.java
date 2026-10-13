package com.practice.url_shortner.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param refreshTokenValidity absolute lifetime of a session, counted from login;
 *                             rotating the refresh token does not extend it
 * @param maxSessionsPerUser   concurrent sessions allowed per user; logging in
 *                             beyond this evicts the oldest session
 */
@ConfigurationProperties(prefix = "app.security")
public record RefreshTokenProperties(
		@DefaultValue("P7D") Duration refreshTokenValidity,
		@DefaultValue("2") int maxSessionsPerUser) {

	public RefreshTokenProperties {
		if (refreshTokenValidity == null || refreshTokenValidity.isZero() || refreshTokenValidity.isNegative()) {
			throw new IllegalArgumentException("app.security.refresh-token-validity must be positive");
		}
		if (maxSessionsPerUser < 1) {
			throw new IllegalArgumentException("app.security.max-sessions-per-user must be at least 1");
		}
	}
}
