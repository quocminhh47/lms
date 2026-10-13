package com.lms.configuration;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

	private String secret;
	private long expirationMs = 3_600_000L;

	@PostConstruct
	public void validate() {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"app.jwt.secret must be set via JWT_SECRET; refusing to start without a signing secret");
		}
		if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException(
					"app.jwt.secret must be at least 32 bytes for HS256");
		}
		if (expirationMs <= 0) {
			throw new IllegalStateException("app.jwt.expiration-ms must be positive");
		}
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public long getExpirationMs() {
		return expirationMs;
	}

	public void setExpirationMs(long expirationMs) {
		this.expirationMs = expirationMs;
	}
}
