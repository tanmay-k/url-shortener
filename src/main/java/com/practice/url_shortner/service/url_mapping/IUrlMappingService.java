package com.practice.url_shortner.service.url_mapping;

import com.practice.url_shortner.model.NewUrlMappingResponseRecord;

public interface IUrlMappingService {

	NewUrlMappingResponseRecord createNewMapping(String longUrl);

	String resolveShortCodeToUrl(String shortCode);

	String resolveShortCodeToUrl(String shortCode, String userAgent);

	void disableMapping(String longUrl);
}
