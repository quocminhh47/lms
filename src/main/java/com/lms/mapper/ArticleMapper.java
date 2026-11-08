package com.lms.mapper;

import org.springframework.stereotype.Component;

import com.lms.dto.article.ArticleManageResponse;
import com.lms.dto.article.ArticlePublicDetailResponse;
import com.lms.dto.article.ArticlePublicSummaryResponse;
import com.lms.entity.Article;
import com.lms.entity.User;

@Component
public class ArticleMapper {

	public ArticlePublicSummaryResponse toPublicSummary(Article article) {
		User author = article.getAuthor();
		return new ArticlePublicSummaryResponse(
				article.getId(),
				article.getSlug(),
				article.getPublishedTitle(),
				article.getPublishedExcerpt(),
				article.getPublishedFeaturedImageUrl(),
				author.getId(),
				author.getFullName(),
				article.getPublishedAt());
	}

	public ArticlePublicDetailResponse toPublicDetail(Article article) {
		User author = article.getAuthor();
		return new ArticlePublicDetailResponse(
				article.getId(),
				article.getSlug(),
				article.getPublishedTitle(),
				article.getPublishedExcerpt(),
				article.getPublishedFeaturedImageUrl(),
				article.getPublishedContentHtml(),
				author.getId(),
				author.getFullName(),
				article.getPublishedAt());
	}

	public ArticleManageResponse toManage(Article article) {
		User author = article.getAuthor();
		User reviewedBy = article.getReviewedBy();
		return new ArticleManageResponse(
				article.getId(),
				article.getSlug(),
				article.getStatus(),
				author.getId(),
				author.getFullName(),
				article.getTitle(),
				article.getExcerpt(),
				article.getFeaturedImageUrl(),
				article.getContentHtml(),
				article.getPublishedTitle(),
				article.getPublishedExcerpt(),
				article.getPublishedFeaturedImageUrl(),
				article.getPublishedContentHtml(),
				article.getRejectionReason(),
				article.getSubmittedAt(),
				reviewedBy != null ? reviewedBy.getId() : null,
				reviewedBy != null ? reviewedBy.getFullName() : null,
				article.getReviewedAt(),
				article.getPublishedAt(),
				article.getArchivedAt(),
				article.getCreatedAt(),
				article.getUpdatedAt());
	}
}
