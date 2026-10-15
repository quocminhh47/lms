package com.lms.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.dto.progress.LessonProgressResponse;
import com.lms.dto.progress.UpsertLessonProgressRequest;
import com.lms.entity.Course;
import com.lms.entity.Lesson;
import com.lms.entity.LessonProgress;
import com.lms.entity.LessonType;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ConflictException;
import com.lms.exception.GlobalExceptionHandler;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.LessonProgressMapper;
import com.lms.repository.CourseRepository;
import com.lms.repository.LessonProgressRepository;
import com.lms.repository.LessonRepository;

@Service
@Transactional(readOnly = true)
public class LessonProgressService {

	private final LessonProgressRepository lessonProgressRepository;
	private final LessonRepository lessonRepository;
	private final CourseRepository courseRepository;
	private final EnrollmentService enrollmentService;
	private final UserService userService;
	private final LessonProgressMapper lessonProgressMapper;

	public LessonProgressService(
			LessonProgressRepository lessonProgressRepository,
			LessonRepository lessonRepository,
			CourseRepository courseRepository,
			EnrollmentService enrollmentService,
			UserService userService,
			LessonProgressMapper lessonProgressMapper) {
		this.lessonProgressRepository = lessonProgressRepository;
		this.lessonRepository = lessonRepository;
		this.courseRepository = courseRepository;
		this.enrollmentService = enrollmentService;
		this.userService = userService;
		this.lessonProgressMapper = lessonProgressMapper;
	}

	public List<LessonProgressResponse> listForCourse(UUID courseId, UUID userId) {
		Course course = courseRepository.findById(courseId)
				.orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
		assertCourseVisible(course);
		enrollmentService.assertCanAccessLearningContent(courseId);
		return lessonProgressRepository.findByUserIdAndCourseId(userId, courseId).stream()
				.map(lessonProgressMapper::toResponse)
				.toList();
	}

	public LessonProgressResponse getForLesson(UUID courseId, UUID lessonId, UUID userId) {
		Lesson lesson = requireLessonInCourse(lessonId, courseId);
		assertCourseVisible(lesson.getSection().getCourse());
		enrollmentService.assertCanAccessLearningContent(courseId);
		return lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
				.map(lessonProgressMapper::toResponse)
				.orElseGet(() -> lessonProgressMapper.unstarted(lessonId));
	}

	@Transactional
	public LessonProgressResponse upsert(
			UUID courseId,
			UUID lessonId,
			UUID userId,
			UpsertLessonProgressRequest request) {
		Lesson lesson = requireLessonInCourse(lessonId, courseId);
		assertCourseVisible(lesson.getSection().getCourse());
		enrollmentService.assertCanAccessLearningContent(courseId);

		return lessonProgressMapper.toResponse(
				persistWatchedDuration(userId, lesson, request.watchedDurationSeconds()));
	}

	@Transactional
	public LessonProgressResponse complete(UUID courseId, UUID lessonId, UUID userId) {
		Lesson lesson = requireLessonInCourse(lessonId, courseId);
		assertCourseVisible(lesson.getSection().getCourse());
		enrollmentService.assertCanAccessLearningContent(courseId);
		assertExplicitCompletionSupported(lesson);

		return lessonProgressMapper.toResponse(persistCompletion(userId, lesson));
	}

	private Lesson requireLessonInCourse(UUID lessonId, UUID courseId) {
		Lesson lesson = lessonRepository.findById(lessonId)
				.orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));
		if (!lesson.getSection().getCourse().getId().equals(courseId)) {
			throw new ResourceNotFoundException("Lesson not found: " + lessonId);
		}
		return lesson;
	}

	private static void assertCourseVisible(Course course) {
		if (!course.isPublished() && !CourseService.currentUserIsAdmin()) {
			throw new ResourceNotFoundException("Course not found: " + course.getId());
		}
	}

	private static void assertExplicitCompletionSupported(Lesson lesson) {
		LessonType type = lesson.getLessonType();
		if (type == LessonType.QUIZ) {
			throw new BusinessRuleException(
					"Quiz lessons cannot be marked complete via this endpoint");
		}
		if (type == LessonType.ASSIGNMENT) {
			throw new BusinessRuleException(
					"Assignment lessons cannot be marked complete via this endpoint");
		}
		if (type != LessonType.VIDEO && type != LessonType.ARTICLE) {
			throw new BusinessRuleException(
					"Only video and article lessons can be marked complete via this endpoint");
		}
	}

	private LessonProgress persistWatchedDuration(UUID userId, Lesson lesson, int watchedDurationSeconds) {
		UUID lessonId = lesson.getId();
		Optional<LessonProgress> existing =
				lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId);
		if (existing.isPresent()) {
			return updateWatchedDuration(existing.get(), watchedDurationSeconds);
		}
		try {
			return createProgressRow(userId, lesson, watchedDurationSeconds, false);
		}
		catch (RuntimeException ex) {
			handleConcurrentInsertFailure(ex);
			return null;
		}
	}

	private LessonProgress persistCompletion(UUID userId, Lesson lesson) {
		UUID lessonId = lesson.getId();
		Optional<LessonProgress> existing =
				lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId);
		if (existing.isPresent()) {
			return markComplete(existing.get());
		}
		try {
			return createProgressRow(userId, lesson, 0, true);
		}
		catch (RuntimeException ex) {
			handleConcurrentInsertFailure(ex);
			return null;
		}
	}

	private void handleConcurrentInsertFailure(RuntimeException ex) {
		if (GlobalExceptionHandler.isLessonProgressUserLessonUniqueViolation(ex)) {
			throw new ConflictException("Progress already exists for this lesson");
		}
		throw ex;
	}

	private LessonProgress updateWatchedDuration(LessonProgress progress, int watchedDurationSeconds) {
		progress.setWatchedDurationSeconds(watchedDurationSeconds);
		progress.setLastAccessedAt(OffsetDateTime.now());
		return lessonProgressRepository.save(progress);
	}

	private LessonProgress markComplete(LessonProgress progress) {
		if (!progress.isCompleted()) {
			progress.setCompleted(true);
		}
		progress.setLastAccessedAt(OffsetDateTime.now());
		return lessonProgressRepository.save(progress);
	}

	private LessonProgress createProgressRow(
			UUID userId,
			Lesson lesson,
			int watchedDurationSeconds,
			boolean completed) {
		LessonProgress progress = new LessonProgress();
		progress.setId(UUID.randomUUID());
		progress.setUser(userService.requireUser(userId));
		progress.setLesson(lessonRepository.getReferenceById(lesson.getId()));
		progress.setWatchedDurationSeconds(watchedDurationSeconds);
		progress.setCompleted(completed);
		progress.setLastAccessedAt(OffsetDateTime.now());
		return lessonProgressRepository.saveAndFlush(progress);
	}
}
