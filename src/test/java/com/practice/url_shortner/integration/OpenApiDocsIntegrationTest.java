package com.practice.url_shortner.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.practice.url_shortner.constants.ErrorCode;

/**
 * Verifies the generated OpenAPI document: Bearer scheme, metadata, server,
 * and that public endpoints opt out of the global security requirement.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiDocs_exposeBearerSchemeInfoAndServer() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("URL Shortener API"))
				.andExpect(jsonPath("$.servers[0].url").value("http://localhost:8080"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
	}

	@Test
	void apiDocs_publicEndpointsHaveNoSecurity_andProtectedEndpointInheritsBearer() throws Exception {
		mockMvc.perform(get("/v3/api-docs/v1")).andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/1/login'].post.security.length()").value(0))
				.andExpect(jsonPath("$.paths['/api/1/user'].post.security.length()").value(0))
				.andExpect(jsonPath("$.paths['/api/1/short-url'].post.responses['401']").exists());
	}

	private static String examples(String path, String status) {
		return "$.paths['" + path + "'].post.responses['" + status + "'].content['application/json'].examples";
	}

	@Test
	void apiDocs_protectedEndpointListsEveryAuthErrorUnder401() throws Exception {
		String ex = examples("/api/1/short-url", "401");
		mockMvc.perform(get("/v3/api-docs/v1")).andExpect(status().isOk())
				.andExpect(jsonPath(ex + ".AUTHENTICATION_REQUIRED.value.errorCode").value("AUTHENTICATION_REQUIRED"))
				.andExpect(jsonPath(ex + ".INVALID_TOKEN.value.errorCode").value("INVALID_TOKEN"))
				.andExpect(jsonPath(ex + ".SESSION_EXPIRED.value.errorCode").value("SESSION_EXPIRED"))
				.andExpect(jsonPath(ex + ".SESSION_EXPIRED.value.message").value(ErrorCode.SESSION_EXPIRED.getMessage()))
				.andExpect(jsonPath(examples("/api/1/short-url", "404") + ".USER_NOT_FOUND").exists());
	}

	@Test
	void apiDocs_publicEndpointsListOnlyTheirOwnCodes() throws Exception {
		mockMvc.perform(get("/v3/api-docs/v1")).andExpect(status().isOk())
				.andExpect(jsonPath(examples("/api/1/login", "401") + ".INVALID_CREDENTIALS").exists())
				.andExpect(jsonPath(examples("/api/1/login", "401") + ".SESSION_EXPIRED").doesNotExist())
				.andExpect(jsonPath(examples("/api/1/user", "400") + ".USER_NAME_NOT_AVAILABLE").exists())
				.andExpect(jsonPath(examples("/api/1/user", "400") + ".INVALID_REQUEST").exists());
	}

	@Test
	void apiDocs_everyEndpointListsRateLimitAndServerError() throws Exception {
		mockMvc.perform(get("/v3/api-docs/v1")).andExpect(status().isOk())
				.andExpect(jsonPath(examples("/api/1/login", "429") + ".TOO_MANY_REQUESTS").exists())
				.andExpect(jsonPath(examples("/api/1/user", "500") + ".INTERNAL_SERVER_ERROR").exists())
				.andExpect(jsonPath(examples("/api/1/short-url", "429") + ".TOO_MANY_REQUESTS.value.message")
						.value(ErrorCode.TOO_MANY_REQUESTS.getMessage()))
				.andExpect(jsonPath("$.components.schemas.ErrorResponse").exists());
	}
}
