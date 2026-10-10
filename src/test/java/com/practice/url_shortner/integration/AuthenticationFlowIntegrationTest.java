package com.practice.url_shortner.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.model.AuthenticationResponseRecord;
import com.practice.url_shortner.utility.JwtUtils;

/**
 * Exercises the real security chain (JwtAuthenticationFilter + SecurityConfig)
 * end to end, to lock in how missing/malformed/invalid tokens are handled on
 * protected, public and redirect routes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationFlowIntegrationTest {

	private static final String PROTECTED_PATH = "/api/1/short-url";
	private static final String PROTECTED_BODY = "{\"longUrl\":\"https://93.184.216.34/\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JwtUtils jwtUtils;

	@Value("${app.security.jwt-secret}")
	private String jwtSecret;

	@Value("${app.security.jwt-issuer}")
	private String jwtIssuer;

	@Value("${app.security.jwt-audience}")
	private String jwtAudience;

	private ResultActions postProtected(String authorizationHeader) throws Exception {
		MockHttpServletRequestBuilder request = post(PROTECTED_PATH).contentType("application/json")
				.content(PROTECTED_BODY);
		if (authorizationHeader != null) {
			request.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}
		return mockMvc.perform(request);
	}

	private void assertError(ResultActions result, ErrorCode expected) throws Exception {
		result.andExpect(status().is(expected.getDefaultHttpStatus().value()))
				.andExpect(jsonPath("$.errorCode").value(expected.getErrorCode()))
				.andExpect(jsonPath("$.message").value(expected.getMessage()));
	}

	private String uniqueUserName() {
		return "user-" + System.nanoTime();
	}

	private void register(String userName, String password) throws Exception {
		mockMvc.perform(post("/api/1/user").contentType("application/json")
				.content("{\"userName\":\"" + userName + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isNoContent());
	}

	// ---- Protected endpoint ----

	@Test
	void protectedEndpoint_returns401AuthenticationRequired_whenHeaderMissing() throws Exception {
		assertError(postProtected(null), ErrorCode.AUTHENTICATION_REQUIRED);
	}

	@ParameterizedTest
	@ValueSource(strings = { " ", "   " })
	void protectedEndpoint_returns401AuthenticationRequired_whenHeaderBlank(String header) throws Exception {
		assertError(postProtected(header), ErrorCode.AUTHENTICATION_REQUIRED);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Basic dXNlcjpwYXNz", "Bearer", "bearer abc", "Token abc", "abc" })
	void protectedEndpoint_returns401InvalidToken_whenHeaderNotBearerToken(String header) throws Exception {
		assertError(postProtected(header), ErrorCode.INVALID_TOKEN);
	}

	@Test
	void protectedEndpoint_returns401InvalidToken_whenTokenMalformed() throws Exception {
		assertError(postProtected("Bearer not.a.jwt"), ErrorCode.INVALID_TOKEN);
	}

	@Test
	void protectedEndpoint_returns401InvalidToken_whenTokenSignedWithWrongSecret() throws Exception {
		String forged = JWT.create().withSubject("someone").withIssuer(jwtIssuer).withAudience(jwtAudience)
				.withExpiresAt(Instant.now().plusSeconds(600)).sign(Algorithm.HMAC256("some-other-secret"));

		assertError(postProtected("Bearer " + forged), ErrorCode.INVALID_TOKEN);
	}

	@Test
	void protectedEndpoint_returns401SessionExpired_whenTokenExpired() throws Exception {
		String expired = jwtUtils.createToken("someone", Instant.now().minusSeconds(3600));

		assertError(postProtected("Bearer " + expired), ErrorCode.SESSION_EXPIRED);
	}

	@Test
	void protectedEndpoint_allowsRequest_whenTokenValid() throws Exception {
		String userName = uniqueUserName();
		register(userName, "password123");

		postProtected("Bearer " + jwtUtils.createToken(userName)).andExpect(status().isOk())
				.andExpect(jsonPath("$.shortCode").isNotEmpty());
	}

	@Test
	void unknownApiRoute_returns401_whenUnauthenticated() throws Exception {
		// anyRequest().authenticated() also covers routes with no controller mapping.
		assertError(mockMvc.perform(get("/api/1/does-not-exist")), ErrorCode.AUTHENTICATION_REQUIRED);
	}

	// ---- Public endpoints (no token must not be rejected) ----

	@Test
	void signUpAndLogin_workWithoutAuthorizationHeader() throws Exception {
		String userName = uniqueUserName();
		register(userName, "password123");

		String body = mockMvc.perform(post("/api/1/login").contentType("application/json")
				.content("{\"userName\":\"" + userName + "\",\"password\":\"password123\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		AuthenticationResponseRecord response = objectMapper.readValue(body, AuthenticationResponseRecord.class);
		assertThat(response.token()).isNotBlank();
	}

	@Test
	void login_returnsInvalidCredentials_notAuthenticationRequired_whenPasswordWrong() throws Exception {
		String userName = uniqueUserName();
		register(userName, "password123");

		assertError(mockMvc.perform(post("/api/1/login").contentType("application/json")
				.content("{\"userName\":\"" + userName + "\",\"password\":\"wrong\"}")),
				ErrorCode.INVALID_CREDENTIALS);
	}

	@Test
	void logoutPath_isNotBlockedBySecurity_whenNoAuthorizationHeader() throws Exception {
		// No controller handles logout yet, so the response status is whatever the app
		// does for an unmapped route (currently a 500 via the generic exception
		// handler). What matters here is that security does not reject it with 401/403.
		int statusCode = mockMvc.perform(post("/api/1/logout")).andReturn().getResponse().getStatus();
		assertThat(statusCode).isNotIn(401, 403);
	}

	@Test
	void publicEndpoint_returns401InvalidToken_whenMalformedAuthorizationHeaderSent() throws Exception {
		// The filter still runs on public /api/** routes, so a malformed header is
		// rejected even where no token is needed.
		assertError(mockMvc.perform(post("/api/1/user").header(HttpHeaders.AUTHORIZATION, "Basic abc")
				.contentType("application/json").content("{\"userName\":\"x\",\"password\":\"y\"}")),
				ErrorCode.INVALID_TOKEN);
	}

	// ---- Redirect endpoint (filter is skipped entirely) ----

	@Test
	void redirect_isNotAuthenticated_whenNoAuthorizationHeader() throws Exception {
		// Unknown code => app-level 404, proving the request got past security.
		// The filter is skipped entirely for redirect routes, so no 401 is returned.
		assertError(mockMvc.perform(get("/zzzzzz99")), ErrorCode.NOT_FOUND);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Basic abc", "Bearer not.a.jwt", "Bearer" })
	void redirect_ignoresAuthorizationHeader_evenIfMalformed(String header) throws Exception {
		assertError(mockMvc.perform(get("/zzzzzz99").header(HttpHeaders.AUTHORIZATION, header)),
				ErrorCode.NOT_FOUND);
	}
}
