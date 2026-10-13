package com.lms.exception;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String USERS_EMAIL_UNIQUE_CONSTRAINT = "uq_users_email";

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(
			ResourceNotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ErrorResponse> handleConflict(
			ConflictException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ErrorResponse> handleBusinessRule(
			BusinessRuleException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(
			MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<String> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::formatFieldError)
				.toList();
		return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), details);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorized(
			UnauthorizedException ex, HttpServletRequest request) {
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), null);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(
			IllegalArgumentException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
	}

	/**
	 * Maps only the users.email uniqueness constraint to 409. Other integrity
	 * violations stay as opaque 500 responses (no SQL details).
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrity(
			DataIntegrityViolationException ex, HttpServletRequest request) {
		if (isUsersEmailUniqueViolation(ex)) {
			return build(
					HttpStatus.CONFLICT,
					"Email already registered",
					request.getRequestURI(),
					null);
		}
		return build(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"A database constraint was violated",
				request.getRequestURI(),
				null);
	}

	public static boolean isUsersEmailUniqueViolation(DataIntegrityViolationException ex) {
		String text = exceptionText(ex).toLowerCase();
		return text.contains(USERS_EMAIL_UNIQUE_CONSTRAINT);
	}

	private static String exceptionText(Throwable ex) {
		StringBuilder sb = new StringBuilder();
		Throwable current = ex;
		while (current != null) {
			if (current.getMessage() != null) {
				sb.append(current.getMessage()).append(' ');
			}
			current = current.getCause();
		}
		return sb.toString();
	}

	private String formatFieldError(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}

	private ResponseEntity<ErrorResponse> build(
			HttpStatus status, String message, String path, List<String> details) {
		ErrorResponse body = new ErrorResponse(
				OffsetDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				path,
				details);
		return ResponseEntity.status(status).body(body);
	}
}
