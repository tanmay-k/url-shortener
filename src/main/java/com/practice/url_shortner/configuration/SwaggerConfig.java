package com.practice.url_shortner.configuration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.model.ErrorResponse;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

	public static final String BEARER_AUTH = "bearerAuth";

	@Bean
	OpenAPI customOpenAPI(@Value("${app.openapi.server-url:http://localhost:8080}") String serverUrl) {
		Components components = new Components().addSecuritySchemes(BEARER_AUTH,
				new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
						.description("JWT returned by POST /api/1/login."));
		ModelConverters.getInstance().read(ErrorResponse.class).forEach(components::addSchemas);
		return new OpenAPI()
				.info(new Info().title("URL Shortener API")
						.description("REST API for registering users, logging in with a JWT, and creating short URLs. "
								+ "Short-code redirects are served at the root path (GET /{shortCode}) and are not listed here.")
						.version("1.0"))
				.addServersItem(new Server().url(serverUrl))
				.components(components)
				.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}

	@Bean
	GroupedOpenApi v1Group(OperationCustomizer errorResponseExamples) {
		return GroupedOpenApi.builder().group("v1").pathsToMatch("/api/1/**")
				.addOperationCustomizer(errorResponseExamples).build();
	}

	/**
	 * Documents error responses as named examples, one per {@link ErrorCode}
	 * grouped by HTTP status, so the text can't drift from what the API returns.
	 * Codes common to all endpoints are added here; endpoint-specific ones come
	 * from {@link ApiErrors}.
	 */
	@Bean
	OperationCustomizer errorResponseExamples() {
		return (operation, handlerMethod) -> {
			Set<ErrorCode> codes = new LinkedHashSet<>(
					List.of(ErrorCode.TOO_MANY_REQUESTS, ErrorCode.INTERNAL_SERVER_ERROR));
			if (operation.getRequestBody() != null) {
				codes.add(ErrorCode.INVALID_REQUEST);
			}
			if (requiresAuthentication(operation)) {
				codes.addAll(List.of(ErrorCode.AUTHENTICATION_REQUIRED, ErrorCode.INVALID_TOKEN,
						ErrorCode.SESSION_EXPIRED));
			}
			ApiErrors declared = handlerMethod.getMethodAnnotation(ApiErrors.class);
			if (declared != null) {
				codes.addAll(List.of(declared.value()));
			}

			if (operation.getResponses() == null) {
				operation.setResponses(new ApiResponses());
			}
			codes.stream().collect(Collectors.groupingBy(code -> String.valueOf(code.getDefaultHttpStatus().value()),
					TreeMap::new, Collectors.toList())).forEach((status, group) -> {
						MediaType mediaType = new MediaType()
								.schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"));
						group.forEach(code -> mediaType.addExamples(code.name(),
								new Example().summary(code.name()).value(
										Map.of("errorCode", code.getErrorCode(), "message", code.getMessage()))));
						operation.getResponses().addApiResponse(status,
								new ApiResponse()
										.description(group.stream().map(ErrorCode::name)
												.collect(Collectors.joining(", ")))
										.content(new Content().addMediaType("application/json", mediaType)));
					});
			return operation;
		};
	}

	/** No operation-level security means the global Bearer requirement applies. */
	private static boolean requiresAuthentication(Operation operation) {
		return operation.getSecurity() == null || !operation.getSecurity().isEmpty();
	}
}
