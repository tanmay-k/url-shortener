package com.practice.url_shortner.constants;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

	INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid credentials provided.", HttpStatus.UNAUTHORIZED),
	AUTHENTICATION_REQUIRED("AUTHENTICATION_REQUIRED", "Authentication is required to access this resource.", HttpStatus.UNAUTHORIZED),
	INVALID_TOKEN("INVALID_TOKEN", "Invalid or malformed authentication token.", HttpStatus.UNAUTHORIZED),
	USER_NAME_NOT_AVAILABLE("USER_NAME_NOT_AVAILABLE", "The username is already taken.", HttpStatus.BAD_REQUEST),
	INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Something went wrong.", HttpStatus.INTERNAL_SERVER_ERROR),
	INVALID_MAPPED_URL("INVALID_MAPPED_URL", "Invalid URL provided for mapping", HttpStatus.BAD_REQUEST),
	USER_NOT_FOUND("USER_NOT_FOUND", "User not found.", HttpStatus.NOT_FOUND),
	NOT_FOUND("NOT_FOUND", "Not found.", HttpStatus.NOT_FOUND),
	TOO_MANY_REQUESTS("TOO_MANY_REQUESTS", "Rate limit exceeded. Please try again later.", HttpStatus.TOO_MANY_REQUESTS);

	private String errorCode;
	private String message;
	private HttpStatus defaultHttpStatus;
}
