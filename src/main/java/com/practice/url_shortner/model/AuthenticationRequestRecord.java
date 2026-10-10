package com.practice.url_shortner.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login credentials")
public record AuthenticationRequestRecord(
		@Schema(description = "Registered username", example = "jane.doe") @NotBlank String userName,
		@Schema(description = "Account password", example = "S3cretPassw0rd!") @NotBlank String password) {

}
