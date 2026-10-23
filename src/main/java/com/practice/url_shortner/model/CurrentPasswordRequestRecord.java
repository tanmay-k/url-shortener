package com.practice.url_shortner.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Re-authentication for a sensitive account action")
public record CurrentPasswordRequestRecord(
		@Schema(description = "The account's current password", example = "S3cretPassw0rd!") @NotBlank String password) {

}
