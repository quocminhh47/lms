package com.lms.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"app.cors.allowed-origins=https://ui.example.com",
		"app.cors.frontend-url=https://ui.example.com/",
})
class CorsPropertiesTest {

	@Autowired
	private CorsProperties corsProperties;

	@Test
	void normalizesOriginsAndMergesFrontendUrlWithoutDuplicates() {
		assertThat(corsProperties.getAllowedOrigins()).containsExactly("https://ui.example.com");
	}
}
