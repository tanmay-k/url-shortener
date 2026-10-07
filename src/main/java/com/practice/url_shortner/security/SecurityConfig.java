package com.practice.url_shortner.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.url_shortner.configuration.RateLimitProperties;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.model.ErrorResponse;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(RateLimitProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final RateLimitingFilter rateLimitingFilter;
	private final UserDetailsService userDetailsService;
	private final ObjectMapper objectMapper;

	//	SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
	//		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	//	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
		authBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
		return authBuilder.build();
	}

	/**
	 * Invoked when an unauthenticated request reaches a route that requires
	 * authentication (e.g. no Authorization header). Replaces Spring Security's
	 * default empty 403 with a 401 and the API's standard JSON error body.
	 */
	@Bean
	AuthenticationEntryPoint authenticationEntryPoint() {
		return (request, response, authException) -> {
			response.setStatus(ErrorCode.AUTHENTICATION_REQUIRED.getDefaultHttpStatus().value());
			response.setContentType("application/json");
			response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.builder()
					.errorCode(ErrorCode.AUTHENTICATION_REQUIRED.getErrorCode())
					.message(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()).build()));
		};
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationEntryPoint authenticationEntryPoint) {
		return http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(
						sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(eh -> eh.authenticationEntryPoint(authenticationEntryPoint))
				.authorizeHttpRequests(auth -> auth.requestMatchers("/api/*/login", "/api/*/logout").permitAll()
						.requestMatchers(HttpMethod.GET, "/{shortCode:[a-zA-Z0-9]{6,10}}").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/*/user").permitAll()
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(rateLimitingFilter, JwtAuthenticationFilter.class)
				.build();
	}
}
