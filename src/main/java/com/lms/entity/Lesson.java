package com.lms.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Lesson {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "title", nullable = false)
	private String title;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "section_id", nullable = false)
	private Section section;

	@Enumerated(EnumType.STRING)
	@Column(name = "lesson_type", nullable = false)
	private LessonType lessonType;

	@Column(name = "content_url", columnDefinition = "TEXT")
	private String contentUrl;

	@Column(name = "duration_seconds")
	private Integer durationSeconds;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder = 0;

	@Enumerated(EnumType.STRING)
	@Column(name = "article_extraction_status")
	private ArticleExtractionStatus articleExtractionStatus;

	@Column(name = "article_extracted_title", columnDefinition = "TEXT")
	private String articleExtractedTitle;

	@Column(name = "article_author", columnDefinition = "TEXT")
	private String articleAuthor;

	@Column(name = "article_published_at")
	private Instant articlePublishedAt;

	@Column(name = "article_featured_image_url", columnDefinition = "TEXT")
	private String articleFeaturedImageUrl;

	@Column(name = "article_content_html", columnDefinition = "TEXT")
	private String articleContentHtml;

	@Column(name = "article_extracted_at")
	private Instant articleExtractedAt;

	@Column(name = "article_extraction_error", columnDefinition = "TEXT")
	private String articleExtractionError;
}
