package com.practice.url_shortner.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.practice.url_shortner.model.NewUrlMappingRequestRecord;
import com.practice.url_shortner.model.NewUrlMappingResponseRecord;
import com.practice.url_shortner.service.url_mapping.IUrlMappingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = { "/api/1" }, version = "1")
public class UrlMappingController {

	private final IUrlMappingService urlMappingService;

	@PostMapping(path = "/short-url", produces = "application/json")
	public NewUrlMappingResponseRecord createNewShortUrl(@Valid @RequestBody NewUrlMappingRequestRecord request) {
		return urlMappingService.createNewMapping(request.longUrl());
	}
}
