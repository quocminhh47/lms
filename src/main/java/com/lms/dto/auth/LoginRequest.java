package com.lms.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login with an email address or a phone number. JSON may use {@code identifier}
 * (preferred) or the legacy {@code email} property for the same value.
 */
public record LoginRequest(
		@NotBlank @Size(max = 254) String identifier,
		@NotBlank @Size(max = 100) String password) {

	@JsonCreator
	public static LoginRequest fromJson(
			@JsonProperty("identifier") String identifier,
			@JsonProperty("email") String legacyEmail,
			@JsonProperty("password") String password) {
		String resolved = resolveIdentifier(identifier, legacyEmail);
		return new LoginRequest(resolved, password != null ? password : "");
	}

	private static String resolveIdentifier(String identifier, String legacyEmail) {
		if (identifier != null && !identifier.isBlank()) {
			return identifier.trim();
		}
		if (legacyEmail != null && !legacyEmail.isBlank()) {
			return legacyEmail.trim();
		}
		return "";
	}
}
