package com.lms.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.lms.dto.lesson.CreateLessonRequest;
import com.lms.dto.lesson.LessonResponse;
import com.lms.dto.lesson.UpdateLessonRequest;
import com.lms.entity.ArticleExtractionStatus;
import com.lms.entity.Course;
import com.lms.entity.Lesson;
import com.lms.entity.LessonType;
import com.lms.entity.Section;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.LessonMapper;
import com.lms.repository.LessonRepository;
import com.lms.util.ReorderSupport;

@Service
@Transactional(readOnly = true)
public class LessonService {

	private final LessonRepository lessonRepository;
	private final SectionService sectionService;
	private final EnrollmentService enrollmentService;
	private final LessonMapper lessonMapper;
	private final LessonArticlePersistenceService lessonArticlePersistenceService;

	public LessonService(
			LessonRepository lessonRepository,
			SectionService sectionService,
			EnrollmentService enrollmentService,
			LessonMapper lessonMapper,
			LessonArticlePersistenceService lessonArticlePersistenceService) {
		this.lessonRepository = lessonRepository;
		this.sectionService = sectionService;
		this.enrollmentService = enrollmentService;
		this.lessonMapper = lessonMapper;
		this.lessonArticlePersistenceService = lessonArticlePersistenceService;
	}

	@Transactional
	public LessonResponse create(UUID courseId, UUID sectionId, CreateLessonRequest request) {
		Section section = sectionService.requireSectionInCourse(sectionId, courseId);
		validateDuration(request.durationSeconds());

		Lesson lesson = new Lesson();
		lesson.setId(UUID.randomUUID());
		lesson.setTitle(request.title().trim());
		lesson.setSection(section);
		lesson.setLessonType(request.lessonType());
		lesson.setContentUrl(blankToNull(request.contentUrl()));
		lesson.setDurationSeconds(request.durationSeconds());
		lesson.setSortOrder(
				request.sortOrder() != null ? request.sortOrder() : nextSortOrderForSection(sectionId));
		validateArticleLesson(lesson.getLessonType(), lesson.getContentUrl());

		if (lesson.getLessonType() == LessonType.ARTICLE && lesson.getContentUrl() != null) {
			lesson.setArticleExtractionStatus(ArticleExtractionStatus.PENDING);
		}

		Lesson saved = lessonRepository.save(lesson);
		scheduleArticleExtractionIfNeeded(saved, false);
		return lessonMapper.toResponse(reloadLesson(saved.getId()));
	}

	@Transactional
	public void reorder(UUID courseId, UUID sectionId, List<UUID> orderedIds) {
		sectionService.requireSectionInCourse(sectionId, courseId);
		List<Lesson> lessons = lessonRepository.findBySectionIdOrderBySortOrderAsc(sectionId);
		List<UUID> existingIds = lessons.stream().map(Lesson::getId).toList();
		ReorderSupport.validateFullReorder(existingIds, orderedIds);
		Map<UUID, Lesson> byId = lessons.stream().collect(Collectors.toMap(Lesson::getId, lesson -> lesson));
		for (int i = 0; i < orderedIds.size(); i++) {
			byId.get(orderedIds.get(i)).setSortOrder(i);
		}
		lessonRepository.saveAll(lessons);
	}

	/**
	 * Public curriculum list for a published course. Summaries omit
	 * {@code contentUrl}; full content requires {@link #getById}.
	 */
	public List<LessonResponse> listBySection(UUID courseId, UUID sectionId) {
		Section section = sectionService.requireSectionInCourse(sectionId, courseId);
		assertCourseVisible(section.getCourse());
		return lessonRepository.findBySectionIdOrderBySortOrderAsc(sectionId).stream()
				.map(lessonMapper::toSummaryResponse)
				.toList();
	}

	public LessonResponse getById(UUID courseId, UUID sectionId, UUID lessonId) {
		Lesson lesson = requireLessonInSection(lessonId, sectionId, courseId);
		assertCourseVisible(lesson.getSection().getCourse());
		enrollmentService.assertCanAccessLearningContent(lesson.getSection().getCourse().getId());
		return lessonMapper.toResponse(lesson);
	}

	/**
	 * Direct lesson lookup with full content. Enforces publish visibility and
	 * enrollment-based learning access (ADMIN bypass).
	 */
	public LessonResponse getById(UUID lessonId) {
		Lesson lesson = lessonRepository.findById(lessonId)
				.orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));
		assertCourseVisible(lesson.getSection().getCourse());
		enrollmentService.assertCanAccessLearningContent(lesson.getSection().getCourse().getId());
		return lessonMapper.toResponse(lesson);
	}

	@Transactional
	public LessonResponse update(
			UUID courseId,
			UUID sectionId,
			UUID lessonId,
			UpdateLessonRequest request) {
		Lesson lesson = requireLessonInSection(lessonId, sectionId, courseId);
		String previousUrl = lesson.getContentUrl();

		if (request.title() != null) {
			String title = request.title().trim();
			if (title.isEmpty()) {
				throw new IllegalArgumentException("title must not be blank");
			}
			lesson.setTitle(title);
		}
		if (request.lessonType() != null) {
			lesson.setLessonType(request.lessonType());
		}
		if (request.contentUrl() != null) {
			lesson.setContentUrl(blankToNull(request.contentUrl()));
		}
		if (request.durationSeconds() != null) {
			validateDuration(request.durationSeconds());
			lesson.setDurationSeconds(request.durationSeconds());
		}
		if (request.sortOrder() != null) {
			lesson.setSortOrder(request.sortOrder());
		}

		validateArticleLesson(lesson.getLessonType(), lesson.getContentUrl());

		boolean urlChanged = !Objects.equals(previousUrl, lesson.getContentUrl());
		if (lesson.getLessonType() == LessonType.ARTICLE && urlChanged) {
			if (lesson.getContentUrl() == null) {
				lesson.setArticleExtractionStatus(null);
				lesson.setArticleExtractionError(null);
				lesson.setArticleContentHtml(null);
				lesson.setArticleExtractedTitle(null);
				lesson.setArticleAuthor(null);
				lesson.setArticlePublishedAt(null);
				lesson.setArticleFeaturedImageUrl(null);
				lesson.setArticleExtractedAt(null);
			} else {
				lesson.setArticleExtractionStatus(ArticleExtractionStatus.PENDING);
				lesson.setArticleExtractionError(null);
			}
		}

		Lesson saved = lessonRepository.save(lesson);
		if (lesson.getLessonType() == LessonType.ARTICLE && urlChanged && lesson.getContentUrl() != null) {
			scheduleArticleExtractionIfNeeded(saved, false);
		}
		return lessonMapper.toResponse(reloadLesson(saved.getId()));
	}

	@Transactional
	public LessonResponse refreshArticle(UUID courseId, UUID sectionId, UUID lessonId) {
		Lesson lesson = requireLessonInSection(lessonId, sectionId, courseId);
		if (lesson.getLessonType() != LessonType.ARTICLE) {
			throw new IllegalArgumentException("Only ARTICLE lessons support article refresh");
		}
		if (lesson.getContentUrl() == null || lesson.getContentUrl().isBlank()) {
			throw new IllegalArgumentException("Article URL is required before refresh");
		}
		lessonArticlePersistenceService.extractAndPersist(lessonId, true);
		return lessonMapper.toResponse(reloadLesson(lessonId));
	}

	private Lesson reloadLesson(UUID lessonId) {
		return lessonRepository.findById(lessonId)
				.orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));
	}

	private void scheduleArticleExtractionIfNeeded(Lesson lesson, boolean refresh) {
		if (lesson.getLessonType() != LessonType.ARTICLE || lesson.getContentUrl() == null) {
			return;
		}
		UUID lessonId = lesson.getId();
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					lessonArticlePersistenceService.extractAndPersist(lessonId, refresh);
				}
			});
		} else {
			lessonArticlePersistenceService.extractAndPersist(lessonId, refresh);
		}
	}

	private Lesson requireLessonInSection(UUID lessonId, UUID sectionId, UUID courseId) {
		sectionService.requireSectionInCourse(sectionId, courseId);
		return lessonRepository.findByIdAndSectionId(lessonId, sectionId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Lesson not found: " + lessonId + " in section " + sectionId));
	}

	private static void assertCourseVisible(Course course) {
		if (!course.isPublished() && !CourseService.currentUserIsAdmin()) {
			throw new ResourceNotFoundException("Course not found: " + course.getId());
		}
	}

	private static void validateDuration(Integer durationSeconds) {
		if (durationSeconds != null && durationSeconds < 0) {
			throw new IllegalArgumentException("durationSeconds must be non-negative");
		}
	}

	private static void validateArticleLesson(LessonType lessonType, String contentUrl) {
		if (lessonType == LessonType.ARTICLE && (contentUrl == null || contentUrl.isBlank())) {
			throw new IllegalArgumentException("contentUrl is required for ARTICLE lessons");
		}
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private int nextSortOrderForSection(UUID sectionId) {
		return lessonRepository.findBySectionIdOrderBySortOrderAsc(sectionId).stream()
				.mapToInt(Lesson::getSortOrder)
				.max()
				.orElse(-1) + 1;
	}
}
