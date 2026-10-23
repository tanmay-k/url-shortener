package com.practice.url_shortner.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class PasswordPolicyPropertiesTest {

	@Configuration
	@EnableConfigurationProperties(PasswordPolicyProperties.class)
	static class TestConfig {
	}

	private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

	@Test
	void defaults_whenNothingIsConfigured() {
		runner.run(context -> {
			PasswordPolicyProperties properties = context.getBean(PasswordPolicyProperties.class);

			assertThat(properties.minLength()).isEqualTo(8);
			assertThat(properties.maxLength()).isEqualTo(72);
			assertThat(properties.requireUppercase()).isFalse();
			assertThat(properties.requireLowercase()).isFalse();
			assertThat(properties.requireDigit()).isFalse();
			assertThat(properties.requireSpecial()).isFalse();
		});
	}

	@Test
	void bindsEveryRuleFromConfiguration() {
		runner.withPropertyValues("app.password-policy.min-length=12", "app.password-policy.max-length=64",
				"app.password-policy.require-uppercase=true", "app.password-policy.require-lowercase=true",
				"app.password-policy.require-digit=true", "app.password-policy.require-special=true")
				.run(context -> {
					PasswordPolicyProperties properties = context.getBean(PasswordPolicyProperties.class);

					assertThat(properties.minLength()).isEqualTo(12);
					assertThat(properties.maxLength()).isEqualTo(64);
					assertThat(properties.requireUppercase()).isTrue();
					assertThat(properties.requireLowercase()).isTrue();
					assertThat(properties.requireDigit()).isTrue();
					assertThat(properties.requireSpecial()).isTrue();
				});
	}

	@Test
	void rejectsAMinimumLengthBelowOne() {
		assertThatThrownBy(() -> new PasswordPolicyProperties(0, 72, false, false, false, false))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsAMaximumLengthBelowTheMinimumOrAboveTheBcryptLimit() {
		assertThatThrownBy(() -> new PasswordPolicyProperties(10, 9, false, false, false, false))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new PasswordPolicyProperties(8, 73, false, false, false, false))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
