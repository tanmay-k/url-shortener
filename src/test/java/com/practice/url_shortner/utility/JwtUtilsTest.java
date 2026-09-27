package com.practice.url_shortner.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

class JwtUtilsTest {

	private static final long TOKEN_VALIDITY_HOURS = 1;
	// tolerance for clock skew between "now" captured in the test and inside JwtUtils
	private static final Duration TOLERANCE = Duration.ofSeconds(5);

	private JwtUtils jwtUtils;

	@BeforeEach
	void setUp() {
		jwtUtils = newJwtUtils("test-secret", "test-issuer", "test-audience", TOKEN_VALIDITY_HOURS);
	}

	private JwtUtils newJwtUtils(String secret, String issuer, String audience, long validity) {
		JwtUtils utils = new JwtUtils();
		ReflectionTestUtils.setField(utils, "jwtSecret", secret);
		ReflectionTestUtils.setField(utils, "issuer", issuer);
		ReflectionTestUtils.setField(utils, "audience", audience);
		ReflectionTestUtils.setField(utils, "tokenValidity", validity);
		return utils;
	}

	@Test
	void createToken_producesTokenWithCorrectSubjectIssuerAudience() {
		String token = jwtUtils.createToken("test-user");

		DecodedJWT decoded = JWT.decode(token);
		assertThat(decoded.getSubject()).isEqualTo("test-user");
		assertThat(decoded.getIssuer()).isEqualTo("test-issuer");
		assertThat(decoded.getAudience()).containsExactly("test-audience");
	}

	@Test
	void createToken_issuedAtIsWithinToleranceWindow() {
		Instant before = Instant.now();
		String token = jwtUtils.createToken("test-user");
		Instant after = Instant.now();

		Instant issuedAt = JWT.decode(token).getIssuedAtAsInstant();

		assertThat(issuedAt).isAfterOrEqualTo(before.minus(TOLERANCE)).isBeforeOrEqualTo(after.plus(TOLERANCE));
	}

	@Test
	void createToken_expiresAtMatchesValidityWindow() {
		Instant before = Instant.now();
		String token = jwtUtils.createToken("test-user");

		Instant expiresAt = JWT.decode(token).getExpiresAtAsInstant();
		Instant expectedExpiry = before.plusSeconds(TOKEN_VALIDITY_HOURS * 3600000);

		assertThat(expiresAt).isCloseTo(expectedExpiry, within(TOLERANCE));
	}

	@Test
	void verifyJwt_succeeds_forValidToken() {
		String token = jwtUtils.createToken("test-user");

		DecodedJWT decoded = jwtUtils.verifyJwt(token);

		assertThat(decoded.getSubject()).isEqualTo("test-user");
	}

	@Test
	void verifyJwt_throwsException_forTamperedOrWrongSecretToken() {
		String token = jwtUtils.createToken("test-user");
		JwtUtils differentSecretUtils = newJwtUtils("different-secret", "test-issuer", "test-audience",
				TOKEN_VALIDITY_HOURS);

		assertThatThrownBy(() -> differentSecretUtils.verifyJwt(token)).isInstanceOf(JWTVerificationException.class);
	}

	@Test
	void verifyJwt_throwsException_forWrongIssuer() {
		String token = jwtUtils.createToken("test-user");
		JwtUtils differentIssuerUtils = newJwtUtils("test-secret", "other-issuer", "test-audience",
				TOKEN_VALIDITY_HOURS);

		assertThatThrownBy(() -> differentIssuerUtils.verifyJwt(token)).isInstanceOf(JWTVerificationException.class);
	}

	@Test
	void verifyJwt_throwsException_forWrongAudience() {
		String token = jwtUtils.createToken("test-user");
		JwtUtils differentAudienceUtils = newJwtUtils("test-secret", "test-issuer", "other-audience",
				TOKEN_VALIDITY_HOURS);

		assertThatThrownBy(() -> differentAudienceUtils.verifyJwt(token))
				.isInstanceOf(JWTVerificationException.class);
	}
}
