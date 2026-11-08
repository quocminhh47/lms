package com.lms.service.article;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArticleHtmlSanitizerTest {

	private final ArticleHtmlSanitizer sanitizer = new ArticleHtmlSanitizer();

	@Test
	void removesScriptsAndEventHandlers() {
		String html = "<p onclick=\"alert(1)\">Hello</p><script>alert(1)</script>";
		String sanitized = sanitizer.sanitize(html, "https://example.com/a");
		assertThat(sanitized).contains("Hello");
		assertThat(sanitized).doesNotContain("script");
		assertThat(sanitized).doesNotContain("onclick");
	}

	@Test
	void blocksJavascriptLinks() {
		String html = "<a href=\"javascript:alert(1)\">bad</a><a href=\"https://example.com\">ok</a>";
		String sanitized = sanitizer.sanitize(html, "https://example.com/a");
		assertThat(sanitized).doesNotContain("javascript:");
		assertThat(sanitized).contains("https://example.com");
	}
}
