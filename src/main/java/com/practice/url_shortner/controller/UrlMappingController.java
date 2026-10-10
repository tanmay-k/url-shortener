package com.practice.url_shortner.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.practice.url_shortner.configuration.ApiErrors;
import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.model.NewUrlMappingRequestRecord;
import com.practice.url_shortner.model.NewUrlMappingResponseRecord;
import com.practice.url_shortner.service.url_mapping.IUrlMappingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = { "/api/1" }, version = "1")
@Tag(name = "Short URLs", description = "Create short URLs. Requires a Bearer token.")
public class UrlMappingController {

	private final IUrlMappingService urlMappingService;

	@Operation(summary = "Create a short URL", description = "Generates a short code for the given long URL, owned by the authenticated user.")
	@ApiResponse(responseCode = "200", description = "Short URL created")
	@ApiErrors({ ErrorCode.USER_NOT_FOUND })
	@PostMapping(path = "/short-url", produces = "application/json")
	public NewUrlMappingResponseRecord createNewShortUrl(@Valid @RequestBody NewUrlMappingRequestRecord request) {
		return urlMappingService.createNewMapping(request.longUrl());
	}
}
