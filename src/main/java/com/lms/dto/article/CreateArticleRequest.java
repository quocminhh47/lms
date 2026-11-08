package com.lms.dto.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateArticleRequest(
		@NotBlank @Size(max = 500) String title,
		String excerpt,
		@NotBlank String contentHtml,
		@Size(max = 2048) String featuredImageUrl,
		Boolean publishImmediately) {
}
