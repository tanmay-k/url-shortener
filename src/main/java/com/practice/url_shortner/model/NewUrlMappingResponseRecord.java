package com.practice.url_shortner.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Created short URL")
public record NewUrlMappingResponseRecord(
		@Schema(description = "Generated 8-character short code", example = "aB3dE5gH") String shortCode,
		@Schema(description = "Full short URL that redirects to the long URL", example = "http://localhost:8080/aB3dE5gH") String shortUrl) {

}
