package com.practice.url_shortner.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.ApiVersionResolver;
import org.springframework.web.accept.PathApiVersionResolver;

@Configuration
public class AppConfiguration {

	@Bean
	ApiVersionResolver apiVersionResolver() {
		return new PathApiVersionResolver(1,
				requestPath -> requestPath.toString().startsWith("/api/"));
	}
}
