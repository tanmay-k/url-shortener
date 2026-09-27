package com.practice.url_shortner.exception_handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.model.ErrorResponse;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void handleApplicationException_mapsCustomExceptionToConfiguredStatusAndBody() {
		CustomException exception = CustomException.builder().errorCode(ErrorCode.USER_NOT_FOUND).build();

		ResponseEntity<ErrorResponse> response = handler.handleApplicationException(exception);

		assertThat(response.getStatusCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.getDefaultHttpStatus());
		assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.getErrorCode());
		assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
	}

	@Test
	void handleApplicationException_mapsDifferentErrorCode() {
		CustomException exception = CustomException.builder().errorCode(ErrorCode.NOT_FOUND).build();

		ResponseEntity<ErrorResponse> response = handler.handleApplicationException(exception);

		assertThat(response.getStatusCode()).isEqualTo(ErrorCode.NOT_FOUND.getDefaultHttpStatus());
		assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND.getErrorCode());
	}

	@Test
	void handleException_mapsGenericExceptionTo500WithInternalServerErrorCode() {
		ResponseEntity<ErrorResponse> response = handler.handleException(new RuntimeException("boom"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode());
	}

	@Test
	void handlerBadCredentialsException_mapsTo401WithInvalidCredentialsCode() {
		ResponseEntity<ErrorResponse> response = handler.handlerBadCredentialsException(new BadCredentialsException("bad"));

		assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS.getDefaultHttpStatus());
		assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS.getErrorCode());
	}
}
