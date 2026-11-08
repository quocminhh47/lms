package com.lms.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lms.entity.ArticleExtractionStatus;
import com.lms.entity.Lesson;
import com.lms.entity.LessonType;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.LessonRepository;
import com.lms.service.article.ArticleExtractionService;
import com.lms.service.article.ArticleFetchException;
import com.lms.service.article.ArticleExtractionService.ExtractedArticleSnapshot;

@Service
public class LessonArticlePersistenceService {

	private final LessonRepository lessonRepository;
	private final ArticleExtractionService articleExtractionService;

	public LessonArticlePersistenceService(
			LessonRepository lessonRepository,
			ArticleExtractionService articleExtractionService) {
		this.lessonRepository = lessonRepository;
		this.articleExtractionService = articleExtractionService;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void extractAndPersist(UUID lessonId, boolean refresh) {
		Lesson lesson = lessonRepository.findById(lessonId)
				.orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));
		if (lesson.getLessonType() != LessonType.ARTICLE) {
			return;
		}
		String sourceUrl = lesson.getContentUrl();
		if (sourceUrl == null || sourceUrl.isBlank()) {
			clearArticleSnapshot(lesson);
			lessonRepository.save(lesson);
			return;
		}

		String previousHtml = lesson.getArticleContentHtml();
		ArticleExtractionStatus previousStatus = lesson.getArticleExtractionStatus();
		String previousTitle = lesson.getArticleExtractedTitle();
		String previousAuthor = lesson.getArticleAuthor();
		Instant previousPublishedAt = lesson.getArticlePublishedAt();
		String previousImage = lesson.getArticleFeaturedImageUrl();
		Instant previousExtractedAt = lesson.getArticleExtractedAt();

		lesson.setArticleExtractionStatus(ArticleExtractionStatus.PENDING);
		lesson.setArticleExtractionError(null);
		lessonRepository.save(lesson);

		try {
			ExtractedArticleSnapshot snapshot = articleExtractionService.extractFromUrl(sourceUrl);
			lesson.setArticleExtractedTitle(snapshot.extractedTitle());
			lesson.setArticleAuthor(snapshot.author());
			lesson.setArticlePublishedAt(snapshot.publishedAt());
			lesson.setArticleFeaturedImageUrl(snapshot.featuredImageUrl());
			lesson.setArticleContentHtml(snapshot.contentHtml());
			lesson.setContentUrl(snapshot.sourceUrl());
			lesson.setArticleExtractionStatus(ArticleExtractionStatus.SUCCESS);
			lesson.setArticleExtractedAt(Instant.now());
			lesson.setArticleExtractionError(null);
		} catch (RuntimeException ex) {
			applyExtractionFailure(lesson, ex, refresh, previousStatus, previousHtml, previousTitle, previousAuthor,
					previousPublishedAt, previousImage, previousExtractedAt);
		}
		lessonRepository.save(lesson);
	}

	private static void applyExtractionFailure(
			Lesson lesson,
			RuntimeException ex,
			boolean refresh,
			ArticleExtractionStatus previousStatus,
			String previousHtml,
			String previousTitle,
			String previousAuthor,
			Instant previousPublishedAt,
			String previousImage,
			Instant previousExtractedAt) {
		lesson.setArticleExtractionStatus(ArticleExtractionStatus.FAILED);
		lesson.setArticleExtractionError(safeClientMessage(ex));
		if (refresh && previousStatus == ArticleExtractionStatus.SUCCESS && previousHtml != null) {
			lesson.setArticleContentHtml(previousHtml);
			lesson.setArticleExtractedTitle(previousTitle);
			lesson.setArticleAuthor(previousAuthor);
			lesson.setArticlePublishedAt(previousPublishedAt);
			lesson.setArticleFeaturedImageUrl(previousImage);
			lesson.setArticleExtractedAt(previousExtractedAt);
		} else if (!refresh) {
			clearArticleContent(lesson);
		}
	}

	private static void clearArticleSnapshot(Lesson lesson) {
		clearArticleContent(lesson);
		lesson.setArticleExtractionStatus(null);
		lesson.setArticleExtractionError(null);
		lesson.setArticleExtractedAt(null);
	}

	private static void clearArticleContent(Lesson lesson) {
		lesson.setArticleExtractedTitle(null);
		lesson.setArticleAuthor(null);
		lesson.setArticlePublishedAt(null);
		lesson.setArticleFeaturedImageUrl(null);
		lesson.setArticleContentHtml(null);
	}

	private static String safeClientMessage(Throwable ex) {
		if (ex instanceof ArticleFetchException articleFetchException) {
			return articleFetchException.getMessage();
		}
		return "Article extraction failed";
	}
}
