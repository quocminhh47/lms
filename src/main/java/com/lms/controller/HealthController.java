package com.lms.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight health checks for container orchestration (no Actuator dependency).
 * Does not expose secrets or internal configuration.
 */
@RestController
public class HealthController {

	private final DataSource dataSource;

	public HealthController(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/** Process liveness — does not check external dependencies. */
	@GetMapping("/health")
	public Map<String, String> liveness() {
		return Map.of("status", "UP");
	}

	/** Readiness — verifies database connectivity (Flyway/Hibernate depend on Postgres). */
	@GetMapping("/health/ready")
	public ResponseEntity<Map<String, String>> readiness() {
		try (Connection connection = dataSource.getConnection()) {
			if (!connection.isValid(3)) {
				return down();
			}
			try (PreparedStatement statement = connection.prepareStatement("SELECT 1");
					ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next() && resultSet.getInt(1) == 1) {
					return ResponseEntity.ok(Map.of("status", "UP"));
				}
			}
		} catch (Exception ignored) {
			// Intentionally omit error details from the HTTP response.
		}
		return down();
	}

	private static ResponseEntity<Map<String, String>> down() {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Map.of("status", "DOWN"));
	}
}
