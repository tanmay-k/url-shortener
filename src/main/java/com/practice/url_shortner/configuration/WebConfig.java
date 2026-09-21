package com.practice.url_shortner.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void configureApiVersioning(ApiVersionConfigurer configurer) {
		configurer
		// Index 1 looks at the second segment (e.g., /api/v1/...)
		// The lambda acts as a Predicate<RequestPath> to see if it should even parse a
		// version
		.usePathSegment(1, requestPath -> {
			String path = requestPath.value();
			// ONLY resolve versions for paths starting with "/api"
			// Shortcodes like "/abcde" will return false, bypass versioning, and won't
			// crash!
			return path.startsWith("/api");
		})
		// Make versioning optional if you want a fallback, or leave it strict for the
		// rest
				.setVersionRequired(false).setDefaultVersion("1");
	}
}
