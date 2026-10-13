package com.practice.url_shortner.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login result")
public record AuthenticationResponseRecord(
		@Schema(description = "JWT to send as 'Authorization: Bearer <token>'", example = "eyJhbGciOiJIUzI1NiJ9...") String token,
		@Schema(description = "Opaque, single-use token to send to POST /api/1/token/refresh for a new token pair. Valid until the session's absolute expiry.") String refreshToken,
		@Schema(description = "Seconds until the JWT expires. Refresh before then.", example = "900") long expiresIn) {}
