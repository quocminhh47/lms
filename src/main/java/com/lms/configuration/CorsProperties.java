package com.lms.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

	private List<String> allowedOrigins = new ArrayList<>(List.of(
			"http://localhost:5173",
			"http://localhost:3000"));

	/**
	 * Optional single production frontend origin ({@code FRONTEND_URL}).
	 * Merged into {@link #allowedOrigins} when set (no wildcards).
	 */
	private String frontendUrl;

	@PostConstruct
	void mergeFrontendUrl() {
		normalizeConfiguredOrigins();
		if (!StringUtils.hasText(frontendUrl)) {
			return;
		}
		String origin = normalizeOrigin(frontendUrl);
		if (allowedOrigins.stream().noneMatch(existing -> existing.equals(origin))) {
			allowedOrigins.add(origin);
		}
	}

	private void normalizeConfiguredOrigins() {
		List<String> normalized = new ArrayList<>();
		for (String origin : allowedOrigins) {
			if (!StringUtils.hasText(origin)) {
				continue;
			}
			String value = normalizeOrigin(origin);
			if (!normalized.contains(value)) {
				normalized.add(value);
			}
		}
		allowedOrigins.clear();
		allowedOrigins.addAll(normalized);
	}

	private static String normalizeOrigin(String origin) {
		String trimmed = origin.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	public List<String> getAllowedOrigins() {
		return allowedOrigins;
	}

	public void setAllowedOrigins(List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	public String getFrontendUrl() {
		return frontendUrl;
	}

	public void setFrontendUrl(String frontendUrl) {
		this.frontendUrl = frontendUrl;
	}
}
