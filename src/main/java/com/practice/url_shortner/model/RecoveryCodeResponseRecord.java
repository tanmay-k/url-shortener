package com.practice.url_shortner.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A new one-time recovery code")
public record RecoveryCodeResponseRecord(
		@Schema(description = "Shown only once, so the user has to store it. Resets the password if the user forgets it, and any earlier code stops working.", example = "ABCD-EFGH-IJKL-MNOP-QRST-UVWX") String recoveryCode) {

}
