package com.lms.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "articles",
		uniqueConstraints = @UniqueConstraint(name = "uq_articles_slug", columnNames = "slug"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Article {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(name = "slug", nullable = false, length = 200)
	private String slug;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ArticleStatus status = ArticleStatus.DRAFT;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "excerpt", columnDefinition = "TEXT")
	private String excerpt;

	@Column(name = "featured_image_url", length = 2048)
	private String featuredImageUrl;

	@Column(name = "content_html", nullable = false, columnDefinition = "TEXT")
	private String contentHtml = "";

	@Column(name = "published_title")
	private String publishedTitle;

	@Column(name = "published_excerpt", columnDefinition = "TEXT")
	private String publishedExcerpt;

	@Column(name = "published_featured_image_url", length = 2048)
	private String publishedFeaturedImageUrl;

	@Column(name = "published_content_html", columnDefinition = "TEXT")
	private String publishedContentHtml;

	@Column(name = "rejection_reason", columnDefinition = "TEXT")
	private String rejectionReason;

	@Column(name = "submitted_at")
	private OffsetDateTime submittedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by_id")
	private User reviewedBy;

	@Column(name = "reviewed_at")
	private OffsetDateTime reviewedAt;

	@Column(name = "published_at")
	private OffsetDateTime publishedAt;

	@Column(name = "archived_at")
	private OffsetDateTime archivedAt;

	@Setter(AccessLevel.NONE)
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Setter(AccessLevel.NONE)
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@PrePersist
	void onPersist() {
		OffsetDateTime now = OffsetDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = OffsetDateTime.now();
	}
}
