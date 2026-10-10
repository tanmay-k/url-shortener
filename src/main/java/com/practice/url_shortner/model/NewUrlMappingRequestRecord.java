package com.practice.url_shortner.model;

import com.practice.url_shortner.validator.SafeUrl;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to shorten a URL")
public record NewUrlMappingRequestRecord(
		@Schema(description = "Long URL to shorten. Must use http or https and must not point to a private address.", example = "https://www.example.com/some/long/path") @SafeUrl String longUrl) {
}
