package com.lms.dto.lesson;

import java.time.Instant;

import com.lms.entity.ArticleExtractionStatus;

public record ArticleSnapshotResponse(
		String sourceUrl,
		String extractedTitle,
		String author,
		Instant publishedAt,
		String featuredImageUrl,
		String contentHtml,
		ArticleExtractionStatus extractionStatus,
		Instant extractedAt,
		String extractionError) {
}
