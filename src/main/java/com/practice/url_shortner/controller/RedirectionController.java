package com.practice.url_shortner.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.practice.url_shortner.service.url_mapping.IUrlMappingService;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RedirectionController {

	private final IUrlMappingService urlMappingService;

	@Hidden
	@GetMapping("/{shortCode}")
	public ResponseEntity<Void> sendRedirect(@PathVariable String shortCode) {
		return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
				.location(URI.create(urlMappingService.resolveShortCodeToUrl(shortCode))).build();
	}
}
