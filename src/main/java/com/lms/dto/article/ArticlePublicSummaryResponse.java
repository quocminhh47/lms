package com.lms.dto.article;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ArticlePublicSummaryResponse(
		UUID id,
		String slug,
		String title,
		String excerpt,
		String featuredImageUrl,
		UUID authorId,
		String authorName,
		OffsetDateTime publishedAt) {
}
