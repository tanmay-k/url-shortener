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

	private final JwtUtils jwtUtils;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String path = request.getServletPath();

		// Skip JWT validation if the request is trying to hit the base redirect path
		// This ensures a 1-character or multi-character shortcode doesn't trigger the
		// filter
		return "GET".equals(request.getMethod()) && !path.startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		//		if(request.getServletPath().equals("/")) {
		//			filterChain.doFilter(request, response);
		//			return;
		//		}

		// Skip authentication for short codes at root
		//		if (isShortCode(request.getRequestURI())) {
		//			filterChain.doFilter(request, response);
		//			return;
		//		}
		//		String path = request.getServletPath();
		//		if( "GET".equals(request.getMethod()) && !path.startsWith("/api/")) {
		//
		//		}

		String header = request.getHeader("Authorization");
		if(StringUtils.isNotBlank(header) && Strings.CS.startsWith(header, "Bearer ")){
			try {
				String token = StringUtils.substring(header, 7);
				DecodedJWT decodedJWT = jwtUtils.verifyJwt(token);
				String userName = decodedJWT.getSubject();
				if(StringUtils.isNotEmpty(userName)){
					var authentication = new UsernamePasswordAuthenticationToken(userName, null,
							Collections.emptyList());
					//					authentication.setAuthenticated(true);
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private boolean isShortCode(String path) {
		// Example: allow any single segment without slash
		return path.matches("^/1/[A-Za-z0-9]+$");
	}
}

