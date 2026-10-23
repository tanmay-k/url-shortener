package com.practice.url_shortner.controller;

import java.security.Principal;

import org.apache.commons.lang3.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.practice.url_shortner.configuration.ApiErrors;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.model.AuthenticationRequestRecord;
import com.practice.url_shortner.model.AuthenticationResponseRecord;
import com.practice.url_shortner.model.ChangePasswordRequestRecord;
import com.practice.url_shortner.model.CreateUserRequest;
import com.practice.url_shortner.model.CurrentPasswordRequestRecord;
import com.practice.url_shortner.model.PasswordResetRequestRecord;
import com.practice.url_shortner.model.RecoveryCodeResponseRecord;
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
@Tag(name = "Users", description = "User registration, login and account management. Registration, login, token refresh and password reset are public.")
public class UserController {

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

	@Operation(summary = "Register a user", description = "Creates a new user account and returns its recovery code. The code is shown only once; it is the only way to reset the password if the user forgets it.")
	@SecurityRequirements({})
	@ApiResponse(responseCode = "201", description = "User created")
	@ApiErrors({ ErrorCode.USER_NAME_NOT_AVAILABLE, ErrorCode.WEAK_PASSWORD })
	@PostMapping("/user")
	public ResponseEntity<RecoveryCodeResponseRecord> createUser(@Valid @RequestBody CreateUserRequest request) {
		return new ResponseEntity<>(userService.register(request), HttpStatus.CREATED);
	}

	@Operation(summary = "Change the password", description = "Replaces the password after checking the current one. Every session of the user is ended, so the user has to log in again.")
	@ApiResponse(responseCode = "204", description = "Password changed")
	@ApiErrors({ ErrorCode.INVALID_CREDENTIALS, ErrorCode.WEAK_PASSWORD, ErrorCode.PASSWORD_UNCHANGED })
	@PutMapping("/user/password")
	public ResponseEntity<Void> changePassword(@Parameter(hidden = true) Principal principal,
			@Valid @RequestBody ChangePasswordRequestRecord request) {
		userService.changePassword(principal.getName(), request);
		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
	}

	@Operation(summary = "Deactivate the account", description = "Disables the account after checking the password and ends every session of the user. The user's short URLs stop working while the account is disabled.")
	@ApiResponse(responseCode = "204", description = "Account deactivated")
	@ApiErrors({ ErrorCode.INVALID_CREDENTIALS })
	@DeleteMapping("/user")
	public ResponseEntity<Void> deactivate(@Parameter(hidden = true) Principal principal,
			@Valid @RequestBody CurrentPasswordRequestRecord request) {
		userService.deactivate(principal.getName(), request.password());
		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
	}

	@Operation(summary = "Issue a new recovery code", description = "Replaces the recovery code after checking the password. The new code is shown only once and the old one stops working. Accounts created before recovery codes existed have to call this to get their first code.")
	@ApiResponse(responseCode = "200", description = "New recovery code issued")
	@ApiErrors({ ErrorCode.INVALID_CREDENTIALS })
	@PostMapping("/user/recovery-code")
	public RecoveryCodeResponseRecord regenerateRecoveryCode(@Parameter(hidden = true) Principal principal,
			@Valid @RequestBody CurrentPasswordRequestRecord request) {
		return userService.regenerateRecoveryCode(principal.getName(), request.password());
	}

	@Operation(summary = "Reset a forgotten password", description = "Sets a new password for the user who presents their recovery code, ends every session of the user, and returns a replacement recovery code. Do not send an Authorization header.")
	@SecurityRequirements({})
	@ApiResponse(responseCode = "200", description = "Password reset; the response holds the replacement recovery code")
	@ApiErrors({ ErrorCode.INVALID_CREDENTIALS, ErrorCode.WEAK_PASSWORD })
	@PostMapping("/password-reset")
	public RecoveryCodeResponseRecord resetPassword(@Valid @RequestBody PasswordResetRequestRecord request) {
		return userService.resetPassword(request);
	}
}
