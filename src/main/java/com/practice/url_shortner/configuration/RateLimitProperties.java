package com.practice.url_shortner.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
		int loginRequestsPerMinute,
		int registrationRequestsPerMinute,
		int urlCreationRequestsPerMinute,
		int redirectRequestsPerMinute) {

	public RateLimitProperties {
		if (loginRequestsPerMinute <= 0) loginRequestsPerMinute = 5;
		if (registrationRequestsPerMinute <= 0) registrationRequestsPerMinute = 5;
		if (urlCreationRequestsPerMinute <= 0) urlCreationRequestsPerMinute = 20;
		if (redirectRequestsPerMinute <= 0) redirectRequestsPerMinute = 60;
	}
}
