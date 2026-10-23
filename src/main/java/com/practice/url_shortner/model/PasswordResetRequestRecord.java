package com.practice.url_shortner.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Password reset using a recovery code")
public record PasswordResetRequestRecord(
		@Schema(description = "Username of the account", example = "jane.doe") @NotBlank String userName,
		@Schema(description = "The account's current recovery code", example = "ABCD-EFGH-IJKL-MNOP-QRST-UVWX") @NotBlank String recoveryCode,
		@Schema(description = "The new password. Must satisfy the password policy.", example = "An0therS3cret!") @NotBlank String newPassword) {

}
