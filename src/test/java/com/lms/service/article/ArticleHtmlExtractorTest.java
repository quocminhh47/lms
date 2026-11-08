package com.lms.service.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ArticleHtmlExtractorTest {

	private final ArticleHtmlExtractor extractor = new ArticleHtmlExtractor();

	@Test
	void extractsArticleElementContent() {
		String html = """
				<html><head><title>Site</title>
				<meta property="og:title" content="Article title" />
				<meta name="author" content="Author Name" />
				</head><body>
				<nav>Menu</nav>
				<article>
				  <h1>Heading</h1>
				  <p>%s</p>
				  <p>Second paragraph with enough body text.</p>
				</article>
				</body></html>
				""".formatted("x".repeat(180));

		var extracted = extractor.extract("https://example.com/post", html);
		assertThat(extracted.title()).isEqualTo("Article title");
		assertThat(extracted.author()).isEqualTo("Author Name");
		assertThat(extracted.contentHtml()).contains("Second paragraph");
		assertThat(extracted.contentHtml()).doesNotContain("Menu");
	}

	@Test
	void failsWhenContentTooShort() {
		String html = "<html><body><article><p>Short</p></article></body></html>";
		assertThatThrownBy(() -> extractor.extract("https://example.com/post", html))
				.isInstanceOf(ArticleFetchException.class);
	}
}
