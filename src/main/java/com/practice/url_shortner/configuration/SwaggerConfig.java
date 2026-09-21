package com.practice.url_shortner.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

	@Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().version("1.0")
                        .description("Demo application"));
    }
}
