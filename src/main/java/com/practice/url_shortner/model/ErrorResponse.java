package com.practice.url_shortner.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@Schema(description = "Error body returned for failed requests. Example values per response are generated from ErrorCode in SwaggerConfig.")
public class ErrorResponse {

	@Schema(description = "Machine-readable error code")
	private String errorCode;

	@Schema(description = "Human-readable message")
	private String message;
}
