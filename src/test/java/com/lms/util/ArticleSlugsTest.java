package com.lms.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArticleSlugsTest {

	@Test
	void slugify_normalizesTitle() {
		assertThat(ArticleSlugs.slugify("Hello World!")).isEqualTo("hello-world");
		assertThat(ArticleSlugs.slugify("  Spring Boot Guide  ")).isEqualTo("spring-boot-guide");
	}

	@Test
	void allocateUnique_addsSuffixWhenTaken() {
		String slug = ArticleSlugs.allocateUnique("Popular Post", candidate -> candidate.equals("popular-post"));
		assertThat(slug).isEqualTo("popular-post-2");
	}
}
