package com.practice.url_shortner.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login result")
public record AuthenticationResponseRecord(
		@Schema(description = "JWT to send as 'Authorization: Bearer <token>'", example = "eyJhbGciOiJIUzI1NiJ9...") String token,
		@Schema(description = "Not currently issued; there is no refresh endpoint", nullable = true) String refreshToken) {}
