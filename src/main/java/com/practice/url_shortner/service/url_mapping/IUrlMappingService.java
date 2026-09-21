package com.practice.url_shortner.service.url_mapping;

import com.practice.url_shortner.model.NewUrlMappingResponseRecord;

public interface IUrlMappingService {

	NewUrlMappingResponseRecord createNewMapping(String longUrl);

	String resolveShortCodeToUrl(String shortCode);

	void disableMapping(String longUrl);
}
