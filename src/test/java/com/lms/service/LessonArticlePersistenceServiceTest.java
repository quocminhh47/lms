package com.lms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lms.entity.ArticleExtractionStatus;
import com.lms.entity.Lesson;
import com.lms.entity.LessonType;
import com.lms.entity.Section;
import com.lms.repository.LessonRepository;
import com.lms.service.article.ArticleExtractionService;
import com.lms.service.article.ArticleExtractionService.ExtractedArticleSnapshot;
import com.lms.service.article.ArticleFetchException;

@ExtendWith(MockitoExtension.class)
class LessonArticlePersistenceServiceTest {

	@Mock
	private LessonRepository lessonRepository;

	@Mock
	private ArticleExtractionService articleExtractionService;

	@InjectMocks
	private LessonArticlePersistenceService service;

	@Test
	void refreshFailurePreservesPreviousSnapshot() {
		UUID lessonId = UUID.randomUUID();
		Lesson lesson = articleLesson(lessonId);
		lesson.setArticleContentHtml("<p>Old content</p>");
		lesson.setArticleExtractionStatus(ArticleExtractionStatus.SUCCESS);
		lesson.setArticleExtractedTitle("Old title");
		lesson.setArticleExtractedAt(Instant.parse("2025-01-01T00:00:00Z"));

		when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
		when(articleExtractionService.extractFromUrl(lesson.getContentUrl()))
				.thenThrow(new ArticleFetchException("blocked"));

		service.extractAndPersist(lessonId, true);

		assertThat(lesson.getArticleExtractionStatus()).isEqualTo(ArticleExtractionStatus.FAILED);
		assertThat(lesson.getArticleContentHtml()).isEqualTo("<p>Old content</p>");
		assertThat(lesson.getArticleExtractedTitle()).isEqualTo("Old title");
		verify(lessonRepository, org.mockito.Mockito.atLeastOnce()).save(lesson);
	}

	private static Lesson articleLesson(UUID lessonId) {
		Section section = new Section();
		section.setId(UUID.randomUUID());
		Lesson lesson = new Lesson();
		lesson.setId(lessonId);
		lesson.setSection(section);
		lesson.setTitle("Article");
		lesson.setLessonType(LessonType.ARTICLE);
		lesson.setContentUrl("https://example.com/article");
		return lesson;
	}
}
