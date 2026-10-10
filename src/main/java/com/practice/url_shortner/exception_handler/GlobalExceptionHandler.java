package com.practice.url_shortner.exception_handler;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.model.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(exception = CustomException.class)
	ResponseEntity<ErrorResponse> handleApplicationException(CustomException exception) {
		log.error("Request failed with exception.", exception);
		return buildResponse(exception.getErrorCode());
	}

	@ExceptionHandler(exception = Exception.class)
	ResponseEntity<ErrorResponse> handleException(Exception exception){
		log.error("Something went wrong.", exception);
		return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(exception = MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
		log.info("Request validation failed: {}", exception.getMessage());
		return buildResponse(ErrorCode.INVALID_REQUEST);
	}

	@ExceptionHandler(exception = BadCredentialsException.class)
	ResponseEntity<ErrorResponse> handlerBadCredentialsException(BadCredentialsException badCredsException){
		log.info("User authentication failed");
		return buildResponse(ErrorCode.INVALID_CREDENTIALS);
	}

	private static ResponseEntity<ErrorResponse> buildResponse(ErrorCode errorCode) {
		return new ResponseEntity<>(
				ErrorResponse.builder().errorCode(errorCode.getErrorCode()).message(errorCode.getMessage()).build(),
				errorCode.getDefaultHttpStatus());
	}
}
