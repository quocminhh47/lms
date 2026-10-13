package com.lms.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lms.configuration.JwtProperties;
import com.lms.entity.UserRole;

class JwtServiceTest {

	private JwtService jwtService;
	private JwtProperties properties;

	@BeforeEach
	void setUp() {
		properties = new JwtProperties();
		properties.setSecret("test-jwt-secret-key-with-32bytes-min");
		properties.setExpirationMs(3_600_000L);
		properties.validate();
		jwtService = new JwtService(properties);
	}

	@Test
	void createToken_roundTripsUserIdAndRole() {
		UUID userId = UUID.randomUUID();
		String token = jwtService.createToken(userId, "student@example.com", UserRole.STUDENT);

		assertThat(jwtService.isValid(token)).isTrue();
		assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
		assertThat(jwtService.extractRole(token)).isEqualTo(UserRole.STUDENT);
	}

	@Test
	void isValid_rejectsTamperedToken() {
		UUID userId = UUID.randomUUID();
		String token = jwtService.createToken(userId, "student@example.com", UserRole.STUDENT);
		String tampered = token.substring(0, token.length() - 4) + "xxxx";

		assertThat(jwtService.isValid(tampered)).isFalse();
	}

	@Test
	void isValid_rejectsExpiredToken() {
		properties.setExpirationMs(1L);
		JwtService shortLived = new JwtService(properties);
		String token = shortLived.createToken(UUID.randomUUID(), "a@b.com", UserRole.STUDENT);

		try {
			Thread.sleep(5L);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		assertThat(shortLived.isValid(token)).isFalse();
	}
}
