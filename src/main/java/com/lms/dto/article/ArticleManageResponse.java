package com.lms.dto.article;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.lms.entity.ArticleStatus;

public record ArticleManageResponse(
		UUID id,
		String slug,
		ArticleStatus status,
		UUID authorId,
		String authorName,
		String title,
		String excerpt,
		String featuredImageUrl,
		String contentHtml,
		String publishedTitle,
		String publishedExcerpt,
		String publishedFeaturedImageUrl,
		String publishedContentHtml,
		String rejectionReason,
		OffsetDateTime submittedAt,
		UUID reviewedById,
		String reviewedByName,
		OffsetDateTime reviewedAt,
		OffsetDateTime publishedAt,
		OffsetDateTime archivedAt,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
