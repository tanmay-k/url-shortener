package com.practice.url_shortner.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RateLimitPropertiesTest {

	@Test
	void compactConstructor_appliesDefaults_whenValuesAreZeroOrNegative() {
		RateLimitProperties properties = new RateLimitProperties(0, -1, 0, -5);

		assertThat(properties.loginRequestsPerMinute()).isEqualTo(5);
		assertThat(properties.registrationRequestsPerMinute()).isEqualTo(5);
		assertThat(properties.urlCreationRequestsPerMinute()).isEqualTo(20);
		assertThat(properties.redirectRequestsPerMinute()).isEqualTo(60);
	}

	@Test
	void compactConstructor_keepsProvidedPositiveValues() {
		RateLimitProperties properties = new RateLimitProperties(10, 15, 25, 100);

		assertThat(properties.loginRequestsPerMinute()).isEqualTo(10);
		assertThat(properties.registrationRequestsPerMinute()).isEqualTo(15);
		assertThat(properties.urlCreationRequestsPerMinute()).isEqualTo(25);
		assertThat(properties.redirectRequestsPerMinute()).isEqualTo(100);
	}
}
