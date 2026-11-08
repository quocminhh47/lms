package com.lms.dto.article;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ArticlePublicDetailResponse(
		UUID id,
		String slug,
		String title,
		String excerpt,
		String featuredImageUrl,
		String contentHtml,
		UUID authorId,
		String authorName,
		OffsetDateTime publishedAt) {
}
