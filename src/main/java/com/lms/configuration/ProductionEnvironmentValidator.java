package com.lms.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

/**
 * Fails fast in production when required secrets are missing (no insecure defaults).
 */
@Component
@Profile("prod")
public class ProductionEnvironmentValidator {

	@Value("${spring.datasource.url:}")
	private String datasourceUrl;

	@Value("${spring.datasource.username:}")
	private String datasourceUsername;

	@Value("${spring.datasource.password:}")
	private String datasourcePassword;

	private final CorsProperties corsProperties;

	public ProductionEnvironmentValidator(CorsProperties corsProperties) {
		this.corsProperties = corsProperties;
	}

	@PostConstruct
	void validate() {
		if (!StringUtils.hasText(datasourceUrl)) {
			throw new IllegalStateException("DB_URL must be set in production");
		}
		if (!StringUtils.hasText(datasourceUsername)) {
			throw new IllegalStateException("DB_USERNAME must be set in production");
		}
		if (!StringUtils.hasText(datasourcePassword)) {
			throw new IllegalStateException("DB_PASSWORD must be set in production");
		}
		validateCors();
	}

	private void validateCors() {
		List<String> origins = corsProperties.getAllowedOrigins();
		if (origins.isEmpty()) {
			throw new IllegalStateException(
					"Configure at least one CORS origin via CORS_ALLOWED_ORIGINS and/or FRONTEND_URL");
		}
		for (String origin : origins) {
			if (!StringUtils.hasText(origin) || "*".equals(origin.trim())) {
				throw new IllegalStateException("Wildcard CORS origins are not allowed in production");
			}
			String trimmed = origin.trim();
			if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
				throw new IllegalStateException("CORS origin must be an absolute http(s) URL");
			}
		}
	}
}
