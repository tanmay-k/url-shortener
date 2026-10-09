package com.practice.url_shortner.security;

import java.io.IOException;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.model.ErrorResponse;
import com.practice.url_shortner.utility.JwtUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtUtils jwtUtils;
	private final ObjectMapper objectMapper;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String path = request.getServletPath();

		// Skip JWT validation if the request is trying to hit the base redirect path
		// This ensures a 1-character or multi-character shortcode doesn't trigger the
		// filter
		return "GET".equals(request.getMethod()) && !path.startsWith("/api/");
	}

	/**
	 * Authenticates the request if it carries a JWT; this filter does not itself
	 * enforce that a request must be authenticated.
	 * <ul>
	 * <li><b>No Authorization header (missing or blank):</b> the request is passed
	 * through untouched, with no authentication set and no rejection. This is
	 * deliberate: public endpoints that carry no token (login, logout, user
	 * sign-up) run through this filter too and must keep working. Rejecting
	 * unauthenticated requests to protected routes is the job of
	 * {@code SecurityConfig}'s {@code anyRequest().authenticated()} rule, so
	 * every new route must be reviewed against that configuration.</li>
	 * <li><b>Header present but not {@code Bearer <token>}, or token invalid or
	 * expired:</b> the request is rejected here with 401 and a JSON
	 * {@link ErrorResponse}. This applies to public endpoints as well, so a stray
	 * malformed {@code Authorization} header on login will get 401.</li>
	 * <li><b>Valid token:</b> the user is placed on the {@code SecurityContext}
	 * and the chain continues.</li>
	 * </ul>
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String header = request.getHeader("Authorization");
		if (StringUtils.isBlank(header)) {
			filterChain.doFilter(request, response);
			return;
		}

		if (!Strings.CS.startsWith(header, BEARER_PREFIX)) {
			log.warn("Authorization header rejected: not a Bearer token");
			sendUnauthorized(response);
			return;
		}

		try {
			String token = StringUtils.substring(header, BEARER_PREFIX.length());
			DecodedJWT decodedJWT = jwtUtils.verifyJwt(token);
			String userName = decodedJWT.getSubject();
			if (StringUtils.isNotEmpty(userName)) {
				var authentication = new UsernamePasswordAuthenticationToken(userName, null,
						Collections.emptyList());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (Exception e) {
			log.warn("JWT verification failed: {}", e.toString());
			sendUnauthorized(response);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void sendUnauthorized(HttpServletResponse response) throws IOException {
		response.setStatus(ErrorCode.INVALID_TOKEN.getDefaultHttpStatus().value());
		response.setContentType("application/json");
		ErrorResponse body = ErrorResponse.builder()
				.errorCode(ErrorCode.INVALID_TOKEN.getErrorCode())
				.message(ErrorCode.INVALID_TOKEN.getMessage())
				.build();
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
