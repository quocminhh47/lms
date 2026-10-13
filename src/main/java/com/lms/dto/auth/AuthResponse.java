package com.lms.dto.auth;

public record AuthResponse(
		String accessToken,
		String tokenType,
		long expiresInMs) {
}
