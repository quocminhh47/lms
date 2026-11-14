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
    @Test
    void preservesEditorFormattingWithoutUnsafeAttributes() {
        String sanitized = sanitizer.sanitize(
                "<p><u>Underline</u><s>Strike</s></p><ol start=\"3\"><li>Third</li></ol>"
                        + "<img src=\"https://example.com/a.png\" onerror=\"alert(1)\">"
                        + "<a href=\"JaVaScRiPt:alert(1)\">Bad</a>",
                "https://example.com");
        assertThat(sanitized).contains("<u>Underline</u>", "<s>Strike</s>", "start=\"3\"");
        assertThat(sanitized).doesNotContain("onerror", "alert(1)");
    }
}
