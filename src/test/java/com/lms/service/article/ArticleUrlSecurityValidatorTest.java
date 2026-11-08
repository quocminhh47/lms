package com.lms.service.article;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ArticleUrlSecurityValidatorTest {

	@Test
	void acceptsPublicHttpsUrl() {
		assertThatCode(() -> ArticleUrlSecurityValidator.validateAndNormalize("https://example.com/post"))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsNonHttpScheme() {
		assertThatThrownBy(() -> ArticleUrlSecurityValidator.validateAndNormalize("file:///etc/passwd"))
				.isInstanceOf(ArticleFetchException.class);
	}

	@Test
	void rejectsLocalhost() {
		assertThatThrownBy(() -> ArticleUrlSecurityValidator.validateAndNormalize("http://localhost/article"))
				.isInstanceOf(ArticleFetchException.class);
	}

	@Test
	void rejectsCredentialsInUrl() {
		assertThatThrownBy(() -> ArticleUrlSecurityValidator.validateAndNormalize("https://user:pass@example.com/a"))
				.isInstanceOf(ArticleFetchException.class);
	}
}
