package com.practice.url_shortner.validator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SafeUrlValidatorTest {

	private SafeUrlValidator validator;

	@BeforeEach
	void setUp() {
		validator = new SafeUrlValidator();
	}

	@Test
	void isValid_returnsFalse_forBlankOrNullValue() {
		assertThat(validator.isValid(null, null)).isFalse();
		assertThat(validator.isValid("", null)).isFalse();
		assertThat(validator.isValid("   ", null)).isFalse();
	}

	@Test
	void isValid_returnsFalse_forMissingOrUnsupportedScheme() {
		assertThat(validator.isValid("example.com", null)).isFalse();
		assertThat(validator.isValid("ftp://93.184.216.34/", null)).isFalse();
	}

	@Test
	void isValid_returnsFalse_forMalformedUri() {
		assertThat(validator.isValid("http://[::1", null)).isFalse();
	}

	@Test
	void isValid_returnsTrue_forHttpsWithPublicIpLiteral() {
		// IP literal - InetAddress.getByName only parses it, no live DNS lookup
		assertThat(validator.isValid("https://93.184.216.34/", null)).isTrue();
	}

	@Test
	void isValid_returnsFalse_forPrivateSiteLocalIpLiteral() {
		assertThat(validator.isValid("http://192.168.1.10/", null)).isFalse();
		assertThat(validator.isValid("http://10.0.0.5/", null)).isFalse();
	}

	@Test
	void isValid_returnsFalse_forLoopbackAndAnyLocalHostLiterals() {
		assertThat(validator.isValid("http://127.0.0.1/", null)).isFalse();
		assertThat(validator.isValid("http://0.0.0.0/", null)).isFalse();
	}

	@Test
	void isValid_returnsFalse_forUnresolvableHost() {
		// .invalid is an IANA-reserved TLD guaranteed to never resolve - deterministic,
		// no dependency on live DNS being available.
		assertThat(validator.isValid("http://this-host-does-not-exist.invalid/", null)).isFalse();
	}
}
