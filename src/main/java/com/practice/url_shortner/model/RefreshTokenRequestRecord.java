package com.practice.url_shortner.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh request")
public record RefreshTokenRequestRecord(
		@Schema(description = "Refresh token from the last login or refresh response. It is single-use.") @NotBlank String refreshToken) {}
