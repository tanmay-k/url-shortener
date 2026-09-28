package com.practice.url_shortner.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.url_shortner.configuration.RateLimitProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private FilterChain filterChain;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private RateLimitingFilter newFilter(RateLimitProperties properties) {
		return new RateLimitingFilter(properties, objectMapper);
	}

	private void stubResponseWriter() throws Exception {
		when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
	}

	@Test
	void doFilterInternal_passesThroughWithoutRateLimiting_whenPathNotRateLimited() throws Exception {
		RateLimitingFilter filter = newFilter(new RateLimitProperties(1, 1, 1, 1));
		when(request.getMethod()).thenReturn("GET");
		when(request.getServletPath()).thenReturn("/api/1/not-rate-limited");

		filter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(response, never()).setStatus(429);
	}

	@Test
	void doFilterInternal_allowsRequestAndSetsRemainingHeader_whenUnderLimit() throws Exception {
		RateLimitingFilter filter = newFilter(new RateLimitProperties(5, 5, 20, 60));
		when(request.getMethod()).thenReturn("GET");
		when(request.getServletPath()).thenReturn("/abc12345");
		when(request.getRemoteAddr()).thenReturn("127.0.0.1");

		filter.doFilterInternal(request, response, filterChain);

		verify(response).setHeader("X-Rate-Limit-Remaining", "59");
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilterInternal_returns429WithRetryAfterAndBody_whenLimitExceeded() throws Exception {
		RateLimitingFilter filter = newFilter(new RateLimitProperties(1, 5, 20, 60));
		when(request.getMethod()).thenReturn("POST");
		when(request.getServletPath()).thenReturn("/api/1/login");
		when(request.getRemoteAddr()).thenReturn("127.0.0.1");
		StringWriter responseBody = new StringWriter();
		when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

		filter.doFilterInternal(request, response, filterChain);
		filter.doFilterInternal(request, response, filterChain);

		verify(filterChain, times(1)).doFilter(request, response);
		verify(response).setStatus(429);
		verify(response).setHeader(org.mockito.ArgumentMatchers.eq("Retry-After"), org.mockito.ArgumentMatchers.any());
		assertThat(responseBody.toString()).contains("TOO_MANY_REQUESTS");
	}

	@Test
	void doFilterInternal_usesUserScopedBucket_forAuthenticatedRequestsToUserScopedEndpoint() throws Exception {
		RateLimitingFilter filter = newFilter(new RateLimitProperties(5, 5, 1, 60));
		when(request.getMethod()).thenReturn("POST");
		when(request.getServletPath()).thenReturn("/api/1/short-url");
		stubResponseWriter();
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("alice", null, List.of()));

		filter.doFilterInternal(request, response, filterChain);
		filter.doFilterInternal(request, response, filterChain);

		verify(filterChain, times(1)).doFilter(request, response);
		verify(response).setStatus(429);
	}

	@Test
	void doFilterInternal_givesDifferentUsersIndependentBuckets_onUserScopedEndpoint() throws Exception {
		RateLimitingFilter filter = newFilter(new RateLimitProperties(5, 5, 1, 60));
		when(request.getMethod()).thenReturn("POST");
		when(request.getServletPath()).thenReturn("/api/1/short-url");

		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("alice", null, List.of()));
		filter.doFilterInternal(request, response, filterChain);

		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("bob", null, List.of()));
		filter.doFilterInternal(request, response, filterChain);

		verify(filterChain, times(2)).doFilter(request, response);
		verify(response, never()).setStatus(429);
	}

	@Test
	void doFilterInternal_resolvesClientIpFromXForwardedForHeader_forIpScopedBuckets() throws Exception {
		RateLimitingFilter filter = newFilter(new RateLimitProperties(5, 5, 5, 1));
		when(request.getMethod()).thenReturn("GET");
		when(request.getServletPath()).thenReturn("/abc12345");
		when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
		stubResponseWriter();

		filter.doFilterInternal(request, response, filterChain);
		filter.doFilterInternal(request, response, filterChain);

		verify(filterChain, times(1)).doFilter(request, response);
		verify(response).setStatus(429);
	}

	@Test
	void doFilterInternal_treatsDifferentForwardedIpsAsIndependentBuckets() throws Exception {
		RateLimitingFilter filter = newFilter(new RateLimitProperties(5, 5, 5, 1));
		when(request.getMethod()).thenReturn("GET");
		when(request.getServletPath()).thenReturn("/abc12345");

		when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");
		filter.doFilterInternal(request, response, filterChain);

		when(request.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9");
		filter.doFilterInternal(request, response, filterChain);

		verify(filterChain, times(2)).doFilter(request, response);
		verify(response, never()).setStatus(429);
	}

	@Test
	void doFilterInternal_usesRemoteAddr_whenXForwardedForHeaderMissing() throws Exception {
		RateLimitingFilter filter = newFilter(new RateLimitProperties(5, 5, 5, 1));
		when(request.getMethod()).thenReturn("GET");
		when(request.getServletPath()).thenReturn("/abc12345");
		when(request.getHeader("X-Forwarded-For")).thenReturn(null);
		when(request.getRemoteAddr()).thenReturn("10.10.10.10");
		stubResponseWriter();

		filter.doFilterInternal(request, response, filterChain);
		filter.doFilterInternal(request, response, filterChain);

		verify(filterChain, times(1)).doFilter(request, response);
		verify(response).setStatus(429);
	}

	@Test
	void doFilterInternal_usesIpScopedBucket_forUnauthenticatedRequestOnUserScopedEndpoint() throws Exception {
		RateLimitingFilter filter = newFilter(new RateLimitProperties(5, 5, 1, 60));
		when(request.getMethod()).thenReturn("POST");
		when(request.getServletPath()).thenReturn("/api/1/short-url");
		when(request.getRemoteAddr()).thenReturn("127.0.0.1");
		stubResponseWriter();
		// No authentication set on the SecurityContext - simulates an anonymous
		// request reaching the filter.

		filter.doFilterInternal(request, response, filterChain);
		filter.doFilterInternal(request, response, filterChain);

		verify(filterChain, times(1)).doFilter(request, response);
		verify(response).setStatus(429);
	}

	@Test
	void doFilterInternal_passesThrough_whenHttpMethodDoesNotMatchAnyLimitedRoute() throws Exception {
		RateLimitingFilter filter = newFilter(new RateLimitProperties(1, 1, 1, 1));
		when(request.getMethod()).thenReturn("DELETE");
		when(request.getServletPath()).thenReturn("/api/1/short-url");

		filter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(response, never()).getWriter();
	}
}
