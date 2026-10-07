package com.practice.url_shortner.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.utility.JwtUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtUtils jwtUtils;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private FilterChain filterChain;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private JwtAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		filter = new JwtAuthenticationFilter(jwtUtils, objectMapper);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void shouldNotFilter_returnsTrue_forGetRequestToNonApiPath() throws Exception {
		when(request.getMethod()).thenReturn("GET");
		when(request.getServletPath()).thenReturn("/abc123");

		assertThat(filter.shouldNotFilter(request)).isTrue();
	}

	@Test
	void shouldNotFilter_returnsFalse_forGetRequestToApiPath() throws Exception {
		when(request.getMethod()).thenReturn("GET");
		when(request.getServletPath()).thenReturn("/api/1/login");

		assertThat(filter.shouldNotFilter(request)).isFalse();
	}

	@Test
	void shouldNotFilter_returnsFalse_forNonGetRequestRegardlessOfPath() throws Exception {
		when(request.getMethod()).thenReturn("POST");
		when(request.getServletPath()).thenReturn("/abc123");

		assertThat(filter.shouldNotFilter(request)).isFalse();
	}

	@Test
	void doFilterInternal_setsAuthentication_whenValidBearerTokenPresent() throws Exception {
		when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
		DecodedJWT decodedJWT = mock(DecodedJWT.class);
		when(decodedJWT.getSubject()).thenReturn("test-user");
		when(jwtUtils.verifyJwt("valid-token")).thenReturn(decodedJWT);

		filter.doFilterInternal(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("test-user");
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilterInternal_passesThroughWithoutAuthentication_whenHeaderMissing() throws Exception {
		when(request.getHeader("Authorization")).thenReturn(null);

		filter.doFilterInternal(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
		verifyNoInteractions(jwtUtils, response);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   " })
	void doFilterInternal_passesThroughWithoutAuthentication_whenHeaderBlank(String header) throws Exception {
		when(request.getHeader("Authorization")).thenReturn(header);

		filter.doFilterInternal(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
		verifyNoInteractions(jwtUtils, response);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Basic xyz", "Bearer", "bearer valid-token", "Token abc", "valid-token" })
	void doFilterInternal_returns401WithErrorBody_whenHeaderNotBearerToken(String header) throws Exception {
		when(request.getHeader("Authorization")).thenReturn(header);
		MockHttpServletResponse realResponse = new MockHttpServletResponse();

		filter.doFilterInternal(request, realResponse, filterChain);

		assertUnauthorizedInvalidToken(realResponse);
		verify(filterChain, never()).doFilter(any(), any());
		verifyNoInteractions(jwtUtils);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void doFilterInternal_returns401_whenBearerTokenEmpty() throws Exception {
		when(request.getHeader("Authorization")).thenReturn("Bearer ");
		when(jwtUtils.verifyJwt("")).thenThrow(new JWTVerificationException("empty"));
		MockHttpServletResponse realResponse = new MockHttpServletResponse();

		filter.doFilterInternal(request, realResponse, filterChain);

		assertUnauthorizedInvalidToken(realResponse);
		verify(filterChain, never()).doFilter(any(), any());
	}

	@Test
	void doFilterInternal_returns401AndSkipsChain_whenJwtVerificationThrows() throws Exception {
		when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
		when(jwtUtils.verifyJwt("bad-token")).thenThrow(new JWTVerificationException("invalid"));
		MockHttpServletResponse realResponse = new MockHttpServletResponse();

		filter.doFilterInternal(request, realResponse, filterChain);

		assertUnauthorizedInvalidToken(realResponse);
		verify(filterChain, never()).doFilter(any(), any());
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void doFilterInternal_returns401_whenTokenExpired() throws Exception {
		when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
		when(jwtUtils.verifyJwt("expired-token"))
				.thenThrow(new TokenExpiredException("expired", Instant.now().minusSeconds(60)));
		MockHttpServletResponse realResponse = new MockHttpServletResponse();

		filter.doFilterInternal(request, realResponse, filterChain);

		assertUnauthorizedInvalidToken(realResponse);
		verify(filterChain, never()).doFilter(any(), any());
	}

	@Test
	void doFilterInternal_doesNotSetAuthentication_whenValidTokenHasNoSubject() throws Exception {
		when(request.getHeader("Authorization")).thenReturn("Bearer no-subject-token");
		DecodedJWT decodedJWT = mock(DecodedJWT.class);
		when(decodedJWT.getSubject()).thenReturn(null);
		when(jwtUtils.verifyJwt("no-subject-token")).thenReturn(decodedJWT);

		filter.doFilterInternal(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
	}

	private void assertUnauthorizedInvalidToken(MockHttpServletResponse realResponse) throws Exception {
		assertThat(realResponse.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		assertThat(realResponse.getContentType()).startsWith("application/json");
		JsonNode body = objectMapper.readTree(realResponse.getContentAsString());
		assertThat(body.get("errorCode").asText()).isEqualTo(ErrorCode.INVALID_TOKEN.getErrorCode());
		assertThat(body.get("message").asText()).isEqualTo(ErrorCode.INVALID_TOKEN.getMessage());
	}
}
