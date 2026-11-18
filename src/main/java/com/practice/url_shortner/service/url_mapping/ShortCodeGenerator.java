package com.practice.url_shortner.service.url_mapping;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

/**
 * Produces random Base62 (alphanumeric) short codes. Uniqueness is <em>not</em> guaranteed here; the caller must
 * handle collisions (see {@link UrlMappingService}).
 */
@Component
public class ShortCodeGenerator {

	public static final int SHORT_CODE_LENGTH = 8;

	/** Built once: constructing a strong {@code SecureRandom} per request is needless overhead. */
	private final RandomStringUtils random = RandomStringUtils.secureStrong();

	public String generate() {
		return random.nextAlphanumeric(SHORT_CODE_LENGTH);
	}
}
