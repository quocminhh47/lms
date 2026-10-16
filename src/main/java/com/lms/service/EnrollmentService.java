package com.lms.service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.dto.enrollment.EnrollmentResponse;
import com.lms.entity.Course;
import com.lms.entity.Enrollment;
import com.lms.entity.EnrollmentStatus;
import com.lms.entity.User;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ConflictException;
import com.lms.exception.GlobalExceptionHandler;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.UnauthorizedException;
import com.lms.mapper.EnrollmentMapper;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.security.LmsUserPrincipal;

@Service
@Transactional(readOnly = true)
public class EnrollmentService {

	private static final BigDecimal FREE_PRICE = BigDecimal.ZERO;
	private static final Set<EnrollmentStatus> LEARNING_ACCESS_STATUSES =
			EnumSet.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED);

	private final EnrollmentRepository enrollmentRepository;
	private final CourseRepository courseRepository;
	private final UserService userService;
	private final EnrollmentMapper enrollmentMapper;

	public EnrollmentService(
			EnrollmentRepository enrollmentRepository,
			CourseRepository courseRepository,
			UserService userService,
			EnrollmentMapper enrollmentMapper) {
		this.enrollmentRepository = enrollmentRepository;
		this.courseRepository = courseRepository;
		this.userService = userService;
		this.enrollmentMapper = enrollmentMapper;
	}

	/**
	 * Enrolls the authenticated user in a published free course as {@code ACTIVE}.
	 * Duplicate {@code (user, course)} rows are rejected with 409; concurrent
	 * races are covered by {@code uq_enrollments_user_course}.
	 */
	@Transactional
	public EnrollmentResponse enroll(UUID courseId, UUID userId) {
		Course course = requirePublishedCourse(courseId);
		assertFreeCourse(course);
		User user = userService.requireUser(userId);

		if (enrollmentRepository.findByUserIdAndCourseId(userId, courseId).isPresent()) {
			throw new ConflictException("Already enrolled in this course");
		}

		Enrollment enrollment = new Enrollment();
		enrollment.setId(UUID.randomUUID());
		enrollment.setUser(user);
		enrollment.setCourse(course);
		enrollment.setStatus(EnrollmentStatus.ACTIVE);

		try {
			Enrollment saved = enrollmentRepository.saveAndFlush(enrollment);
			return enrollmentMapper.toResponse(saved);
		}
		catch (DataIntegrityViolationException ex) {
			if (GlobalExceptionHandler.isEnrollmentUserCourseUniqueViolation(ex)) {
				throw new ConflictException("Already enrolled in this course");
			}
			throw ex;
		}
	}

	public List<EnrollmentResponse> listMine(UUID userId) {
		return enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(userId).stream()
				.map(enrollmentMapper::toResponse)
				.toList();
	}

	public EnrollmentResponse getMine(UUID enrollmentId, UUID userId) {
		Enrollment enrollment = enrollmentRepository.findByIdAndUserId(enrollmentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Enrollment not found: " + enrollmentId));
		return enrollmentMapper.toResponse(enrollment);
	}

	/**
	 * Guards lesson content / playback URLs. ADMIN bypasses enrollment.
	 * Guests receive 401; authenticated users without ACTIVE/COMPLETED
	 * enrollment in {@code courseId} receive 403.
	 */
	public void assertCanAccessLearningContent(UUID courseId) {
		if (CourseService.currentUserIsAdmin()) {
			return;
		}
		UUID userId = requireAuthenticatedUserId();
		if (!hasLearningAccess(userId, courseId)) {
			throw new AccessDeniedException("Enrollment required to access lesson content");
		}
	}

	public boolean hasLearningAccess(UUID userId, UUID courseId) {
		return enrollmentRepository.existsByUserIdAndCourseIdAndStatusIn(
				userId,
				courseId,
				LEARNING_ACCESS_STATUSES);
	}

	private static UUID requireAuthenticatedUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof LmsUserPrincipal principal)) {
			throw new UnauthorizedException("Authentication required");
		}
		return principal.getId();
	}

	private Course requirePublishedCourse(UUID courseId) {
		Course course = courseRepository.findById(courseId)
				.orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
		if (!course.isPublished()) {
			throw new ResourceNotFoundException("Course not found: " + courseId);
		}
		return course;
	}

	private static void assertFreeCourse(Course course) {
		if (course.getPrice() == null || course.getPrice().compareTo(FREE_PRICE) != 0) {
			throw new BusinessRuleException(
					"Only free courses can be enrolled in the MVP; course price must be 0");
		}
	}
}
