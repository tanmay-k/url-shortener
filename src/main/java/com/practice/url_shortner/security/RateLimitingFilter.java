package com.practice.url_shortner.security;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.url_shortner.configuration.RateLimitProperties;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.model.ErrorResponse;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

	private final RateLimitProperties rateLimitProperties;
	private final ObjectMapper objectMapper;

	private final ConcurrentHashMap<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Bucket> userBuckets = new ConcurrentHashMap<>();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String method = request.getMethod();
		String path = request.getServletPath();

		Integer limitPerMinute = getLimitForRequest(method, path);
		if (limitPerMinute == null) {
			filterChain.doFilter(request, response);
			return;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isAuthenticated = authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);

		String key;
		ConcurrentHashMap<String, Bucket> store;

		if (isAuthenticated && isUserScopedEndpoint(method, path)) {
			key = authentication.getName();
			store = userBuckets;
		} else {
			key = resolveClientIp(request);
			store = ipBuckets;
		}

		Bucket bucket = store.computeIfAbsent(key, k -> createBucket(limitPerMinute));
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

		if (probe.isConsumed()) {
			response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
			filterChain.doFilter(request, response);
		} else {
			long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
			log.debug("Rate limit exceeded for key={} path={}", key, path);
			sendRateLimitResponse(response, retryAfterSeconds);
		}
	}

	private Integer getLimitForRequest(String method, String path) {
		if ("POST".equals(method) && path.matches("/api/[^/]+/login")) {
			return rateLimitProperties.loginRequestsPerMinute();
		}
		if ("POST".equals(method) && path.matches("/api/[^/]+/user")) {
			return rateLimitProperties.registrationRequestsPerMinute();
		}
		if ("POST".equals(method) && path.matches("/api/[^/]+/short-url")) {
			return rateLimitProperties.urlCreationRequestsPerMinute();
		}
		if ("GET".equals(method) && path.matches("/[a-zA-Z0-9]{6,10}")) {
			return rateLimitProperties.redirectRequestsPerMinute();
		}
		return null;
	}

	private boolean isUserScopedEndpoint(String method, String path) {
		return "POST".equals(method) && path.matches("/api/[^/]+/short-url");
	}

	private String resolveClientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (StringUtils.isNotBlank(forwardedFor)) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private Bucket createBucket(int requestsPerMinute) {
		return Bucket.builder()
				.addLimit(limit -> limit.capacity(requestsPerMinute)
						.refillGreedy(requestsPerMinute, Duration.ofMinutes(1)))
				.build();
	}

	private void sendRateLimitResponse(HttpServletResponse response, long retryAfterSeconds) throws IOException {
		response.setStatus(ErrorCode.TOO_MANY_REQUESTS.getDefaultHttpStatus().value());
		response.setContentType("application/json");
		response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
		ErrorResponse body = ErrorResponse.builder()
				.errorCode(ErrorCode.TOO_MANY_REQUESTS.getErrorCode())
				.message(ErrorCode.TOO_MANY_REQUESTS.getMessage())
				.build();
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
