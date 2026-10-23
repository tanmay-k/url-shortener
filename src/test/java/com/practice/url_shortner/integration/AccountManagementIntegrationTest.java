package com.practice.url_shortner.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.entity.RefreshTokenEntity;
import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.model.AuthenticationResponseRecord;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.repository.IRefreshTokenRepository;
import com.practice.url_shortner.repository.IUserRepository;
import com.practice.url_shortner.service.token.IRefreshTokenService;
import com.auth0.jwt.JWT;
import java.time.Instant;

/**
 * Password change, recovery-code reset and deactivation, end to end, with the
 * default password policy (8 characters minimum) and 2 sessions per user.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = { "app.rate-limit.login-requests-per-minute=1000",
		"app.rate-limit.registration-requests-per-minute=1000" })
class AccountManagementIntegrationTest {

	private static final String PASSWORD = "password123";
	private static final String NEW_PASSWORD = "brand-new-password";
	private static final String PROTECTED_PATH = "/api/1/short-url";
	private static final String LONG_URL = "https://93.184.216.34/";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private IUserRepository userRepository;

	@Autowired
	private IRefreshTokenRepository refreshTokenRepository;

	@Autowired
	private IRefreshTokenService refreshTokenService;

	// ---- helpers ----

	private static String json(String... keyValues) {
		StringBuilder body = new StringBuilder("{");
		for (int i = 0; i < keyValues.length; i += 2) {
			if (i > 0) {
				body.append(',');
			}
			body.append('"').append(keyValues[i]).append("\":\"").append(keyValues[i + 1]).append('"');
		}
		return body.append('}').toString();
	}

	private ResultActions registerRaw(String userName, String password) throws Exception {
		return mockMvc.perform(post("/api/1/user").contentType("application/json")
				.content(json("userName", userName, "password", password)));
	}

	private String newUserName() {
		return "user-" + System.nanoTime();
	}

	/** Registers a user and returns the recovery code. */
	private String register(String userName) throws Exception {
		String body = registerRaw(userName, PASSWORD).andExpect(status().isCreated()).andReturn().getResponse()
				.getContentAsString();
		return objectMapper.readTree(body).get("recoveryCode").asText();
	}

	private ResultActions loginRaw(String userName, String password) throws Exception {
		return mockMvc.perform(post("/api/1/login").contentType("application/json")
				.content(json("userName", userName, "password", password)));
	}

	private AuthenticationResponseRecord login(String userName, String password) throws Exception {
		String body = loginRaw(userName, password).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();
		return objectMapper.readValue(body, AuthenticationResponseRecord.class);
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/1/token/refresh").contentType("application/json")
				.content(json("refreshToken", refreshToken)));
	}

	private ResultActions createShortUrl(String accessToken) throws Exception {
		return mockMvc.perform(post(PROTECTED_PATH).contentType("application/json")
				.content(json("longUrl", LONG_URL)).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
	}

	private String createShortCode(String accessToken) throws Exception {
		String body = createShortUrl(accessToken).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();
		return objectMapper.readTree(body).get("shortCode").asText();
	}

	private ResultActions changePassword(String accessToken, String current, String next) throws Exception {
		return mockMvc.perform(put("/api/1/user/password").contentType("application/json")
				.content(json("currentPassword", current, "newPassword", next))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
	}

	private ResultActions deactivate(String accessToken, String password) throws Exception {
		return mockMvc.perform(delete("/api/1/user").contentType("application/json")
				.content(json("password", password)).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
	}

	private ResultActions newRecoveryCode(String accessToken, String password) throws Exception {
		return mockMvc.perform(post("/api/1/user/recovery-code").contentType("application/json")
				.content(json("password", password)).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
	}

	private ResultActions reset(String userName, String recoveryCode, String newPassword) throws Exception {
		return mockMvc.perform(post("/api/1/password-reset").contentType("application/json")
				.content(json("userName", userName, "recoveryCode", recoveryCode, "newPassword", newPassword)));
	}

	private String resetOk(String userName, String recoveryCode, String newPassword) throws Exception {
		String body = reset(userName, recoveryCode, newPassword).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();
		return objectMapper.readTree(body).get("recoveryCode").asText();
	}

	private void assertError(ResultActions result, ErrorCode expected) throws Exception {
		result.andExpect(status().is(expected.getDefaultHttpStatus().value()))
				.andExpect(jsonPath("$.errorCode").value(expected.getErrorCode()));
	}

	private UserEntity stored(String userName) {
		return userRepository.findByUsername(userName).orElseThrow();
	}

	// ---- registration ----

	@Test
	void register_returnsARecoveryCode_andStoresOnlyItsHash() throws Exception {
		String userName = newUserName();

		String code = register(userName);

		assertThat(code).matches("([A-Z2-7]{4}-){5}[A-Z2-7]{4}");
		assertThat(stored(userName).getRecoveryCodeHash()).isNotBlank().doesNotContain(code)
				.doesNotContain(code.replace("-", ""));
	}

	@Test
	void register_rejectsAPasswordBelowTheMinimumLength_andCreatesNoUser() throws Exception {
		String userName = newUserName();

		assertError(registerRaw(userName, "short"), ErrorCode.WEAK_PASSWORD);

		assertThat(userRepository.findByUsername(userName)).isEmpty();
	}

	@Test
	void register_aTakenUsername_isStillRejected() throws Exception {
		String userName = newUserName();
		register(userName);

		assertError(registerRaw(userName, PASSWORD), ErrorCode.USER_NAME_NOT_AVAILABLE);
	}

	// ---- change password ----

	@Test
	void changePassword_endsEverySession_andOnlyTheNewPasswordWorksAfterwards() throws Exception {
		String userName = newUserName();
		register(userName);
		AuthenticationResponseRecord first = login(userName, PASSWORD);
		AuthenticationResponseRecord second = login(userName, PASSWORD);

		changePassword(first.token(), PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

		// Both sessions are dead, including the one that made the request.
		assertError(createShortUrl(first.token()), ErrorCode.AUTHENTICATION_REQUIRED);
		assertError(createShortUrl(second.token()), ErrorCode.AUTHENTICATION_REQUIRED);
		assertError(refresh(first.refreshToken()), ErrorCode.INVALID_REFRESH_TOKEN);
		assertError(refresh(second.refreshToken()), ErrorCode.INVALID_REFRESH_TOKEN);

		assertError(loginRaw(userName, PASSWORD), ErrorCode.INVALID_CREDENTIALS);
		AuthenticationResponseRecord fresh = login(userName, NEW_PASSWORD);
		createShortUrl(fresh.token()).andExpect(status().isOk());
	}

	@Test
	void changePassword_leavesOtherUsersSessionsAlone() throws Exception {
		String userName = newUserName();
		register(userName);
		String otherName = newUserName();
		register(otherName);
		AuthenticationResponseRecord session = login(userName, PASSWORD);
		AuthenticationResponseRecord other = login(otherName, PASSWORD);

		changePassword(session.token(), PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

		createShortUrl(other.token()).andExpect(status().isOk());
		refresh(other.refreshToken()).andExpect(status().isOk());
	}

	@Test
	void changePassword_rejectsAWrongCurrentPassword_andKeepsEverythingAsItWas() throws Exception {
		String userName = newUserName();
		register(userName);
		AuthenticationResponseRecord session = login(userName, PASSWORD);

		assertError(changePassword(session.token(), "not-the-password", NEW_PASSWORD), ErrorCode.INVALID_CREDENTIALS);

		createShortUrl(session.token()).andExpect(status().isOk());
		login(userName, PASSWORD);
		assertError(loginRaw(userName, NEW_PASSWORD), ErrorCode.INVALID_CREDENTIALS);
	}

	@Test
	void changePassword_rejectsAWeakNewPassword() throws Exception {
		String userName = newUserName();
		register(userName);
		AuthenticationResponseRecord session = login(userName, PASSWORD);

		assertError(changePassword(session.token(), PASSWORD, "short"), ErrorCode.WEAK_PASSWORD);

		createShortUrl(session.token()).andExpect(status().isOk());
	}

	@Test
	void changePassword_rejectsReusingTheCurrentPassword() throws Exception {
		String userName = newUserName();
		register(userName);
		AuthenticationResponseRecord session = login(userName, PASSWORD);

		assertError(changePassword(session.token(), PASSWORD, PASSWORD), ErrorCode.PASSWORD_UNCHANGED);

		createShortUrl(session.token()).andExpect(status().isOk());
	}

	@Test
	void changePassword_requiresAuthentication() throws Exception {
		mockMvc.perform(put("/api/1/user/password").contentType("application/json")
				.content(json("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD)))
				.andExpect(status().isUnauthorized());
	}

	// ---- reset with a recovery code ----

	@Test
	void reset_setsTheNewPassword_endsEverySession_andReturnsAReplacementCode() throws Exception {
		String userName = newUserName();
		String code = register(userName);
		AuthenticationResponseRecord session = login(userName, PASSWORD);

		String replacement = resetOk(userName, code, NEW_PASSWORD);

		assertThat(replacement).isNotEqualTo(code).matches("([A-Z2-7]{4}-){5}[A-Z2-7]{4}");
		assertError(createShortUrl(session.token()), ErrorCode.AUTHENTICATION_REQUIRED);
		assertError(refresh(session.refreshToken()), ErrorCode.INVALID_REFRESH_TOKEN);
		assertError(loginRaw(userName, PASSWORD), ErrorCode.INVALID_CREDENTIALS);
		login(userName, NEW_PASSWORD);
	}

	@Test
	void reset_codeWorksOnlyOnce_andTheReplacementWorksNext() throws Exception {
		String userName = newUserName();
		String code = register(userName);

		String replacement = resetOk(userName, code, NEW_PASSWORD);

		assertError(reset(userName, code, "yet-another-password"), ErrorCode.INVALID_CREDENTIALS);
		login(userName, NEW_PASSWORD);
		resetOk(userName, replacement, "yet-another-password");
		login(userName, "yet-another-password");
	}

	@Test
	void reset_acceptsTheCodeTypedInLowerCaseWithoutDashes() throws Exception {
		String userName = newUserName();
		String code = register(userName);

		resetOk(userName, code.replace("-", "").toLowerCase(), NEW_PASSWORD);

		login(userName, NEW_PASSWORD);
	}

	@Test
	void reset_aWrongCode_andAnUnknownUser_failIdentically() throws Exception {
		String userName = newUserName();
		register(userName);

		String wrongCode = reset(userName, "AAAA-AAAA-AAAA-AAAA-AAAA-AAAA", NEW_PASSWORD).andReturn().getResponse()
				.getContentAsString();
		String unknownUser = reset("ghost-" + System.nanoTime(), "AAAA-AAAA-AAAA-AAAA-AAAA-AAAA", NEW_PASSWORD)
				.andReturn().getResponse().getContentAsString();

		assertError(reset(userName, "AAAA-AAAA-AAAA-AAAA-AAAA-AAAA", NEW_PASSWORD), ErrorCode.INVALID_CREDENTIALS);
		assertThat(wrongCode).isEqualTo(unknownUser);
		login(userName, PASSWORD);
	}

	@Test
	void reset_aWeakNewPassword_isRejected_andTheCodeIsNotSpent() throws Exception {
		String userName = newUserName();
		String code = register(userName);

		assertError(reset(userName, code, "short"), ErrorCode.WEAK_PASSWORD);

		resetOk(userName, code, NEW_PASSWORD);
	}

	@Test
	void reset_failsForAUserWhoHasNoRecoveryCodeYet() throws Exception {
		String userName = newUserName();
		register(userName);
		UserEntity user = stored(userName);
		user.setRecoveryCodeHash(null);
		userRepository.save(user);

		assertError(reset(userName, "AAAA-AAAA-AAAA-AAAA-AAAA-AAAA", NEW_PASSWORD), ErrorCode.INVALID_CREDENTIALS);
	}

	@Test
	void reset_ignoresAStaleAuthorizationHeader() throws Exception {
		String userName = newUserName();
		String code = register(userName);

		mockMvc.perform(post("/api/1/password-reset").header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
				.contentType("application/json")
				.content(json("userName", userName, "recoveryCode", code, "newPassword", NEW_PASSWORD)))
				.andExpect(status().isOk());
	}

	// ---- recovery code regeneration ----

	@Test
	void newRecoveryCode_replacesTheOldOne_andKeepsSessionsAlive() throws Exception {
		String userName = newUserName();
		String oldCode = register(userName);
		AuthenticationResponseRecord session = login(userName, PASSWORD);

		String body = newRecoveryCode(session.token(), PASSWORD).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();
		String newCode = objectMapper.readTree(body).get("recoveryCode").asText();

		assertThat(newCode).isNotEqualTo(oldCode);
		createShortUrl(session.token()).andExpect(status().isOk());
		assertError(reset(userName, oldCode, NEW_PASSWORD), ErrorCode.INVALID_CREDENTIALS);
		resetOk(userName, newCode, NEW_PASSWORD);
	}

	@Test
	void newRecoveryCode_givesAnExistingUserWithoutOneTheirFirstCode() throws Exception {
		String userName = newUserName();
		register(userName);
		UserEntity user = stored(userName);
		user.setRecoveryCodeHash(null);
		userRepository.save(user);
		AuthenticationResponseRecord session = login(userName, PASSWORD);

		String body = newRecoveryCode(session.token(), PASSWORD).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();

		resetOk(userName, objectMapper.readTree(body).get("recoveryCode").asText(), NEW_PASSWORD);
	}

	@Test
	void newRecoveryCode_requiresTheCurrentPassword() throws Exception {
		String userName = newUserName();
		String code = register(userName);
		AuthenticationResponseRecord session = login(userName, PASSWORD);

		assertError(newRecoveryCode(session.token(), "not-the-password"), ErrorCode.INVALID_CREDENTIALS);

		resetOk(userName, code, NEW_PASSWORD);
	}

	// ---- deactivation ----

	@Test
	void deactivate_disablesTheAccount_andEndsEverySession() throws Exception {
		String userName = newUserName();
		register(userName);
		AuthenticationResponseRecord first = login(userName, PASSWORD);
		AuthenticationResponseRecord second = login(userName, PASSWORD);

		deactivate(first.token(), PASSWORD).andExpect(status().isNoContent());

		assertThat(stored(userName).isEnabled()).isFalse();
		assertError(createShortUrl(first.token()), ErrorCode.AUTHENTICATION_REQUIRED);
		assertError(createShortUrl(second.token()), ErrorCode.AUTHENTICATION_REQUIRED);
		assertError(refresh(first.refreshToken()), ErrorCode.INVALID_REFRESH_TOKEN);
		assertError(refresh(second.refreshToken()), ErrorCode.INVALID_REFRESH_TOKEN);
	}

	@Test
	void deactivate_aDisabledUserCannotLogIn_andLooksLikeAWrongPassword() throws Exception {
		String userName = newUserName();
		register(userName);
		AuthenticationResponseRecord session = login(userName, PASSWORD);
		deactivate(session.token(), PASSWORD).andExpect(status().isNoContent());

		String rightPassword = loginRaw(userName, PASSWORD).andExpect(status().isUnauthorized()).andReturn()
				.getResponse().getContentAsString();
		String wrongPassword = loginRaw(userName, "not-the-password").andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		JsonNode body = objectMapper.readTree(rightPassword);
		assertThat(body.get("errorCode").asText()).isEqualTo(ErrorCode.INVALID_CREDENTIALS.getErrorCode());
		assertThat(rightPassword).isEqualTo(wrongPassword);
	}

	@Test
	void deactivate_aDisabledUserCannotResetThePasswordEither() throws Exception {
		String userName = newUserName();
		String code = register(userName);
		AuthenticationResponseRecord session = login(userName, PASSWORD);
		deactivate(session.token(), PASSWORD).andExpect(status().isNoContent());

		assertError(reset(userName, code, NEW_PASSWORD), ErrorCode.INVALID_CREDENTIALS);
		assertThat(stored(userName).isEnabled()).isFalse();
	}

	@Test
	void deactivate_rejectsAWrongPassword_andLeavesTheAccountEnabled() throws Exception {
		String userName = newUserName();
		register(userName);
		AuthenticationResponseRecord session = login(userName, PASSWORD);

		assertError(deactivate(session.token(), "not-the-password"), ErrorCode.INVALID_CREDENTIALS);

		assertThat(stored(userName).isEnabled()).isTrue();
		createShortUrl(session.token()).andExpect(status().isOk());
	}

	@Test
	void deactivate_requiresAuthentication() throws Exception {
		mockMvc.perform(delete("/api/1/user").contentType("application/json").content(json("password", PASSWORD)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deactivate_stopsTheUsersShortUrlsFromResolving_butNotOtherUsers() throws Exception {
		String ownerName = newUserName();
		register(ownerName);
		AuthenticationResponseRecord owner = login(ownerName, PASSWORD);
		String ownerCode = createShortCode(owner.token());
		String otherName = newUserName();
		register(otherName);
		String otherCode = createShortCode(login(otherName, PASSWORD).token());
		mockMvc.perform(get("/" + ownerCode)).andExpect(status().isMovedPermanently())
				.andExpect(header().string("Location", LONG_URL));

		deactivate(owner.token(), PASSWORD).andExpect(status().isNoContent());

		assertError(mockMvc.perform(get("/" + ownerCode)), ErrorCode.NOT_FOUND);
		mockMvc.perform(get("/" + otherCode)).andExpect(status().isMovedPermanently());
	}

	@Test
	void deactivate_theShortUrlsResolveAgain_whenTheAccountIsEnabledAgain() throws Exception {
		String ownerName = newUserName();
		register(ownerName);
		AuthenticationResponseRecord owner = login(ownerName, PASSWORD);
		String ownerCode = createShortCode(owner.token());
		deactivate(owner.token(), PASSWORD).andExpect(status().isNoContent());
		assertError(mockMvc.perform(get("/" + ownerCode)), ErrorCode.NOT_FOUND);

		UserEntity user = stored(ownerName);
		user.setEnabled(true);
		userRepository.save(user);

		mockMvc.perform(get("/" + ownerCode)).andExpect(status().isMovedPermanently());
	}

	// ---- defence in depth ----

	@Test
	void startSession_refusesAUserWhoWasDeactivatedAfterLoginAuthenticatedThem() throws Exception {
		// Login authenticates before startSession takes the user lock; this is the state a
		// deactivation that commits in between leaves behind.
		String userName = newUserName();
		register(userName);
		UserEntity user = stored(userName);
		user.setEnabled(false);
		userRepository.save(user);

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> refreshTokenService.startSession(userName))
				.isInstanceOf(CustomException.class).extracting(ex -> ((CustomException) ex).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_CREDENTIALS);
		assertThat(refreshTokenRepository.findActiveSessions(user.getId(), Instant.now())).isEmpty();
	}

	@Test
	void changePassword_alsoRevokesTheAccessTokenOfASessionPastItsRefreshExpiry() throws Exception {
		String userName = newUserName();
		register(userName);
		AuthenticationResponseRecord expired = login(userName, PASSWORD);
		AuthenticationResponseRecord current = login(userName, PASSWORD);
		// The first session's refresh expiry passes, but the access token it last issued is still valid.
		RefreshTokenEntity link = refreshTokenRepository
				.findFirstByCurrentAccessJti(JWT.decode(expired.token()).getId()).orElseThrow();
		link.setExpiresAt(Instant.now().minusSeconds(1));
		refreshTokenRepository.save(link);
		createShortUrl(expired.token()).andExpect(status().isOk());

		changePassword(current.token(), PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

		assertError(createShortUrl(expired.token()), ErrorCode.AUTHENTICATION_REQUIRED);
	}

	@Test
	void deactivate_alsoRevokesTheAccessTokenOfASessionPastItsRefreshExpiry() throws Exception {
		String userName = newUserName();
		register(userName);
		AuthenticationResponseRecord session = login(userName, PASSWORD);
		RefreshTokenEntity link = refreshTokenRepository
				.findFirstByCurrentAccessJti(JWT.decode(session.token()).getId()).orElseThrow();
		link.setExpiresAt(Instant.now().minusSeconds(1));
		refreshTokenRepository.save(link);

		deactivate(session.token(), PASSWORD).andExpect(status().isNoContent());

		assertError(createShortUrl(session.token()), ErrorCode.AUTHENTICATION_REQUIRED);
	}

	@Test
	void refresh_rejectsAUserWhoWasDisabledWithoutTheirSessionsBeingRevoked() throws Exception {
		String userName = newUserName();
		register(userName);
		AuthenticationResponseRecord session = login(userName, PASSWORD);
		UserEntity user = stored(userName);
		user.setEnabled(false);
		userRepository.save(user);

		assertError(refresh(session.refreshToken()), ErrorCode.INVALID_REFRESH_TOKEN);

		// The session is revoked by the failed attempt, so re-enabling does not bring it back.
		user.setEnabled(true);
		userRepository.save(user);
		assertError(refresh(session.refreshToken()), ErrorCode.INVALID_REFRESH_TOKEN);
	}
}
