package com.practice.url_shortner.controller;

import java.util.Collections;

import org.apache.commons.lang3.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.practice.url_shortner.configuration.ApiErrors;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.model.AuthenticationRequestRecord;
import com.practice.url_shortner.model.AuthenticationResponseRecord;
import com.practice.url_shortner.model.CreateUserRequest;
import com.practice.url_shortner.model.RefreshTokenRequestRecord;
import com.practice.url_shortner.service.user.IUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = { "/api/1" }, version = "1")
@Tag(name = "Users", description = "User registration and login. Both endpoints are public.")
public class UserController {

	private final UserDetailsManager userDetailsManager;
	private final PasswordEncoder passwordEncoder;
	private final IUserService userService;

	@Operation(summary = "Log in", description = "Exchanges a username and password for a JWT. Send it as 'Authorization: Bearer <token>' on protected endpoints.")
	@SecurityRequirements({})
	@ApiResponse(responseCode = "200", description = "Login successful")
	@ApiErrors({ ErrorCode.INVALID_CREDENTIALS })
	@PostMapping("/login")
	public AuthenticationResponseRecord login(@Valid @RequestBody AuthenticationRequestRecord credentials) {
		return userService.login(credentials);
	}

	@Operation(summary = "Refresh the session", description = "Exchanges a refresh token for a new JWT and a new refresh token. The presented refresh token is spent; presenting it again revokes the whole session. Do not send an Authorization header.")
	@SecurityRequirements({})
	@ApiResponse(responseCode = "200", description = "New token pair issued")
	@ApiErrors({ ErrorCode.INVALID_REFRESH_TOKEN, ErrorCode.SESSION_EXPIRED })
	@PostMapping("/token/refresh")
	public AuthenticationResponseRecord refresh(@Valid @RequestBody RefreshTokenRequestRecord request) {
		return userService.refresh(request.refreshToken());
	}

	@Operation(summary = "Log out", description = "Revokes the JWT sent in the Authorization header and ends its session, so the session's refresh token stops working too. The JWT is rejected from then on, even though it has not expired yet.")
	@ApiResponse(responseCode = "204", description = "Token revoked")
	@ApiErrors({ ErrorCode.AUTHENTICATION_REQUIRED, ErrorCode.INVALID_TOKEN,
			ErrorCode.SESSION_EXPIRED })
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
		// JwtAuthenticationFilter has already validated the header and token.
		userService.logout(Strings.CS.removeStart(authorization, "Bearer "));
		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
	}

	@Operation(summary = "Register a user", description = "Creates a new user account.")
	@SecurityRequirements({})
	@ApiResponse(responseCode = "204", description = "User created")
	@ApiErrors({ ErrorCode.USER_NAME_NOT_AVAILABLE })
	@PostMapping("/user")
	public ResponseEntity<Void> createUser(@Valid @RequestBody CreateUserRequest request) {
		userDetailsManager.createUser(
				new User(request.userName(), passwordEncoder.encode(request.password()), Collections.emptyList()));

		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
	}
}
