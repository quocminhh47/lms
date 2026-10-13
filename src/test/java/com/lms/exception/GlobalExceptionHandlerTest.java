package com.lms.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Mock
	private HttpServletRequest request;

	@Test
	void dataIntegrity_usersEmailUnique_returns409() {
		when(request.getRequestURI()).thenReturn("/api/auth/register");
		DataIntegrityViolationException ex = new DataIntegrityViolationException(
				"could not execute statement",
				new SQLException("Unique index or primary key violation: \"UQ_USERS_EMAIL\""));

		ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(ex, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo("Email already registered");
		assertThat(response.getBody().path()).isEqualTo("/api/auth/register");
	}

	@Test
	void dataIntegrity_unrelatedConstraint_returns500() {
		when(request.getRequestURI()).thenReturn("/api/courses");
		DataIntegrityViolationException ex = new DataIntegrityViolationException(
				"could not execute statement",
				new SQLException("Unique index or primary key violation: \"uq_courses_title\""));

		ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(ex, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo("A database constraint was violated");
		assertThat(response.getBody().message()).doesNotContain("uq_courses_title");
	}

	@Test
	void isUsersEmailUniqueViolation_requiresConstraintName() {
		assertThat(GlobalExceptionHandler.isUsersEmailUniqueViolation(
				new DataIntegrityViolationException("violation of uq_users_email"))).isTrue();
		assertThat(GlobalExceptionHandler.isUsersEmailUniqueViolation(
				new DataIntegrityViolationException("duplicate email on users"))).isFalse();
	}
}
