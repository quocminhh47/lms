package com.lms.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.dto.lesson.CreateLessonRequest;
import com.lms.dto.lesson.LessonResponse;
import com.lms.dto.lesson.UpdateLessonRequest;
import com.lms.entity.Course;
import com.lms.entity.Lesson;
import com.lms.entity.Section;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.LessonMapper;
import com.lms.repository.LessonRepository;

@Service
@Transactional(readOnly = true)
public class LessonService {

	private final LessonRepository lessonRepository;
	private final SectionService sectionService;
	private final EnrollmentService enrollmentService;
	private final LessonMapper lessonMapper;

	public LessonService(
			LessonRepository lessonRepository,
			SectionService sectionService,
			EnrollmentService enrollmentService,
			LessonMapper lessonMapper) {
		this.lessonRepository = lessonRepository;
		this.sectionService = sectionService;
		this.enrollmentService = enrollmentService;
		this.lessonMapper = lessonMapper;
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
		lesson.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);

		return lessonMapper.toResponse(lessonRepository.save(lesson));
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

		return lessonMapper.toResponse(lessonRepository.save(lesson));
	}

	private Lesson requireLessonInSection(UUID lessonId, UUID sectionId, UUID courseId) {
		// Ensure the section belongs to the course first (hierarchy).
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

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
