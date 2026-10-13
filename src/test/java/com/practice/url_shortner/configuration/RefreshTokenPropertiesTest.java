package com.practice.url_shortner.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RefreshTokenPropertiesTest {

	@Test
	void acceptsValidValues() {
		RefreshTokenProperties properties = new RefreshTokenProperties(Duration.ofDays(7), 2);

		assertThat(properties.refreshTokenValidity()).isEqualTo(Duration.ofDays(7));
		assertThat(properties.maxSessionsPerUser()).isEqualTo(2);
	}

	@Test
	void rejectsFewerThanOneSession() {
		assertThatThrownBy(() -> new RefreshTokenProperties(Duration.ofDays(7), 0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsNonPositiveValidity() {
		assertThatThrownBy(() -> new RefreshTokenProperties(Duration.ZERO, 2))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new RefreshTokenProperties(Duration.ofDays(-1), 2))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
