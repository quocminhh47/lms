package com.lms.mapper;

import org.springframework.stereotype.Component;

import com.lms.dto.lesson.ArticleSnapshotResponse;
import com.lms.dto.lesson.LessonResponse;
import com.lms.entity.Lesson;
import com.lms.entity.LessonType;
import com.lms.service.CourseService;

@Component
public class LessonMapper {

	public LessonResponse toResponse(Lesson lesson) {
		return toResponse(lesson, CourseService.currentUserIsAdmin());
	}

	public LessonResponse toResponse(Lesson lesson, boolean includeAdminArticleFields) {
		return new LessonResponse(
				lesson.getId(),
				lesson.getSection().getId(),
				lesson.getSection().getCourse().getId(),
				lesson.getTitle(),
				lesson.getLessonType(),
				lesson.getContentUrl(),
				lesson.getDurationSeconds(),
				lesson.getSortOrder(),
				toArticleSnapshot(lesson, includeAdminArticleFields));
	}

	/**
	 * Public curriculum summary: same shape as {@link LessonResponse} but never
	 * includes {@code contentUrl} or article HTML (playback / article payload).
	 */
	public LessonResponse toSummaryResponse(Lesson lesson) {
		ArticleSnapshotResponse articleSummary = null;
		if (lesson.getLessonType() == LessonType.ARTICLE) {
			articleSummary = new ArticleSnapshotResponse(
					null,
					lesson.getArticleExtractedTitle(),
					null,
					null,
					null,
					null,
					lesson.getArticleExtractionStatus(),
					lesson.getArticleExtractedAt(),
					CourseService.currentUserIsAdmin() ? lesson.getArticleExtractionError() : null);
		}
		return new LessonResponse(
				lesson.getId(),
				lesson.getSection().getId(),
				lesson.getSection().getCourse().getId(),
				lesson.getTitle(),
				lesson.getLessonType(),
				null,
				lesson.getDurationSeconds(),
				lesson.getSortOrder(),
				articleSummary);
	}

	private static ArticleSnapshotResponse toArticleSnapshot(Lesson lesson, boolean includeAdminArticleFields) {
		if (lesson.getLessonType() != LessonType.ARTICLE) {
			return null;
		}
		return new ArticleSnapshotResponse(
				lesson.getContentUrl(),
				lesson.getArticleExtractedTitle(),
				lesson.getArticleAuthor(),
				lesson.getArticlePublishedAt(),
				lesson.getArticleFeaturedImageUrl(),
				lesson.getArticleContentHtml(),
				lesson.getArticleExtractionStatus(),
				lesson.getArticleExtractedAt(),
				includeAdminArticleFields ? lesson.getArticleExtractionError() : null);
	}
}
