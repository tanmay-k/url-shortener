package com.practice.url_shortner.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Password change details")
public record ChangePasswordRequestRecord(
		@Schema(description = "The account's current password", example = "S3cretPassw0rd!") @NotBlank String currentPassword,
		@Schema(description = "The new password. Must satisfy the password policy and differ from the current one.", example = "An0therS3cret!") @NotBlank String newPassword) {

}
