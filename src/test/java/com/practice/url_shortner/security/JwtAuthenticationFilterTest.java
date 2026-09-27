package com.practice.url_shortner.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
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

	private JwtAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		filter = new JwtAuthenticationFilter(jwtUtils);
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
	void doFilterInternal_doesNotSetAuthentication_whenHeaderMissing() throws Exception {
		when(request.getHeader("Authorization")).thenReturn(null);

		filter.doFilterInternal(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilterInternal_doesNotSetAuthentication_whenHeaderNotBearerScheme() throws Exception {
		when(request.getHeader("Authorization")).thenReturn("Basic xyz");

		filter.doFilterInternal(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilterInternal_returns401AndSkipsChain_whenJwtVerificationThrows() throws Exception {
		when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
		when(jwtUtils.verifyJwt("bad-token")).thenThrow(new JWTVerificationException("invalid"));

		filter.doFilterInternal(request, response, filterChain);

		verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		verify(filterChain, never()).doFilter(any(), any());
	}
}
