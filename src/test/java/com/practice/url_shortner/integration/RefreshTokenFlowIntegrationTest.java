package com.practice.url_shortner.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.entity.RefreshTokenEntity;
import com.practice.url_shortner.model.AuthenticationResponseRecord;
import com.practice.url_shortner.repository.IRefreshTokenRepository;

/**
 * Runs with the default of 2 sessions per user and a 7 day refresh lifetime.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = { "app.rate-limit.login-requests-per-minute=1000",
		"app.rate-limit.registration-requests-per-minute=1000" })
class RefreshTokenFlowIntegrationTest {

	private static final String PROTECTED_PATH = "/api/1/short-url";
	private static final String PROTECTED_BODY = "{\"longUrl\":\"https://93.184.216.34/\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private IRefreshTokenRepository refreshTokenRepository;

	private String registerUser() throws Exception {
		String userName = "user-" + System.nanoTime();
		mockMvc.perform(post("/api/1/user").contentType("application/json")
				.content("{\"userName\":\"" + userName + "\",\"password\":\"password123\"}"))
				.andExpect(status().isCreated());
		return userName;
	}

	private AuthenticationResponseRecord login(String userName) throws Exception {
		String body = mockMvc.perform(post("/api/1/login").contentType("application/json")
				.content("{\"userName\":\"" + userName + "\",\"password\":\"password123\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(body, AuthenticationResponseRecord.class);
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/1/token/refresh").contentType("application/json")
				.content("{\"refreshToken\":\"" + refreshToken + "\"}"));
	}

	private AuthenticationResponseRecord refreshOk(String refreshToken) throws Exception {
		String body = refresh(refreshToken).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();
		return objectMapper.readValue(body, AuthenticationResponseRecord.class);
	}

	private ResultActions useAccessToken(String accessToken) throws Exception {
		return mockMvc.perform(post(PROTECTED_PATH).contentType("application/json").content(PROTECTED_BODY)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
	}

	@Test
	void login_returnsOpaqueRefreshToken_andRefreshIssuesNewWorkingPair() throws Exception {
		String userName = registerUser();
		AuthenticationResponseRecord first = login(userName);
		assertThat(first.refreshToken()).isNotBlank().doesNotContain(".");
		assertThat(first.expiresIn()).isEqualTo(3600);

		AuthenticationResponseRecord second = refreshOk(first.refreshToken());

		assertThat(second.refreshToken()).isNotBlank().isNotEqualTo(first.refreshToken());
		assertThat(second.token()).isNotEqualTo(first.token());
		useAccessToken(second.token()).andExpect(status().isOk());
	}

	@Test
	void refreshToken_isStoredHashedOnly() throws Exception {
		AuthenticationResponseRecord session = login(registerUser());

		assertThat(refreshTokenRepository.findAll()).extracting(RefreshTokenEntity::getTokenHash)
				.doesNotContain(session.refreshToken());
	}

	@Test
	void refresh_ignoresAnExpiredAuthorizationHeader() throws Exception {
		AuthenticationResponseRecord session = login(registerUser());

		mockMvc.perform(post("/api/1/token/refresh").contentType("application/json")
				.header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
				.content("{\"refreshToken\":\"" + session.refreshToken() + "\"}")).andExpect(status().isOk());
	}

	@Test
	void refresh_rejectsUnknownToken() throws Exception {
		refresh("not-a-real-token").andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_REFRESH_TOKEN.getErrorCode()));
	}

	@Test
	void refresh_rejectsBlankToken() throws Exception {
		refresh("").andExpect(status().isBadRequest());
	}

	@Test
	void replayingASpentToken_revokesTheWholeSession() throws Exception {
		AuthenticationResponseRecord first = login(registerUser());
		AuthenticationResponseRecord second = refreshOk(first.refreshToken());

		refresh(first.refreshToken()).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_REFRESH_TOKEN.getErrorCode()));

		// The legitimate holder of the newest token is cut off too, refresh token and access token.
		refresh(second.refreshToken()).andExpect(status().isUnauthorized());
		useAccessToken(second.token()).andExpect(status().isUnauthorized());
		useAccessToken(first.token()).andExpect(status().isUnauthorized());
	}

	@Test
	void refresh_rejectsAnExpiredSession_andRotationDoesNotExtendIt() throws Exception {
		AuthenticationResponseRecord first = login(registerUser());
		AuthenticationResponseRecord second = refreshOk(first.refreshToken());

		RefreshTokenEntity live = refreshTokenRepository
				.findFirstByCurrentAccessJti(JWT.decode(second.token()).getId()).orElseThrow();
		RefreshTokenEntity original = refreshTokenRepository
				.findFirstByCurrentAccessJti(JWT.decode(first.token()).getId()).orElseThrow();
		assertThat(live.getFamilyId()).isEqualTo(original.getFamilyId());
		assertThat(live.getExpiresAt()).isEqualTo(original.getExpiresAt());

		live.setExpiresAt(Instant.now().minusSeconds(1));
		refreshTokenRepository.save(live);

		refresh(second.refreshToken()).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value(ErrorCode.SESSION_EXPIRED.getErrorCode()));
	}

	@Test
	void loggingInBeyondTheSessionLimit_evictsTheOldestSession() throws Exception {
		String userName = registerUser();
		AuthenticationResponseRecord oldest = login(userName);
		AuthenticationResponseRecord middle = login(userName);
		AuthenticationResponseRecord newest = login(userName);

		// Limit is 2: the first session is gone immediately, access token included.
		refresh(oldest.refreshToken()).andExpect(status().isUnauthorized());
		useAccessToken(oldest.token()).andExpect(status().isUnauthorized());

		useAccessToken(middle.token()).andExpect(status().isOk());
		useAccessToken(newest.token()).andExpect(status().isOk());
		refreshOk(middle.refreshToken());
	}

	@Test
	void evictionIsPerUser() throws Exception {
		AuthenticationResponseRecord other = login(registerUser());
		String userName = registerUser();
		login(userName);
		login(userName);
		login(userName);

		refreshOk(other.refreshToken());
	}

	@Test
	void logout_revokesTheSessionsRefreshToken() throws Exception {
		AuthenticationResponseRecord session = login(registerUser());

		mockMvc.perform(post("/api/1/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
				.andExpect(status().isNoContent());

		refresh(session.refreshToken()).andExpect(status().isUnauthorized());
	}

	@Test
	void logout_afterRotation_revokesTheSessionAndEarlierAccessToken() throws Exception {
		AuthenticationResponseRecord first = login(registerUser());
		AuthenticationResponseRecord second = refreshOk(first.refreshToken());

		mockMvc.perform(post("/api/1/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + second.token()))
				.andExpect(status().isNoContent());

		refresh(second.refreshToken()).andExpect(status().isUnauthorized());
		useAccessToken(first.token()).andExpect(status().isUnauthorized());
	}

	@Test
	void logout_leavesTheUsersOtherSessionAlone() throws Exception {
		String userName = registerUser();
		AuthenticationResponseRecord a = login(userName);
		AuthenticationResponseRecord b = login(userName);

		mockMvc.perform(post("/api/1/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + a.token()))
				.andExpect(status().isNoContent());

		refreshOk(b.refreshToken());
	}
}
