package com.lms.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.dto.course.CourseResponse;
import com.lms.dto.course.CreateCourseRequest;
import com.lms.dto.course.UpdateCourseRequest;
import com.lms.entity.Course;
import com.lms.entity.User;
import com.lms.entity.UserRole;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.CourseMapper;
import com.lms.repository.CourseRepository;
import com.lms.repository.LessonRepository;

@Service
@Transactional(readOnly = true)
public class CourseService {

	private static final BigDecimal FREE_PRICE = BigDecimal.ZERO;
	private static final String ROLE_ADMIN = "ROLE_ADMIN";

	private final CourseRepository courseRepository;
	private final LessonRepository lessonRepository;
	private final CourseMapper courseMapper;
	private final UserService userService;

	public CourseService(
			CourseRepository courseRepository,
			LessonRepository lessonRepository,
			CourseMapper courseMapper,
			UserService userService) {
		this.courseRepository = courseRepository;
		this.lessonRepository = lessonRepository;
		this.courseMapper = courseMapper;
		this.userService = userService;
	}

	@Transactional
	public CourseResponse create(CreateCourseRequest request) {
		validateMvpPrice(request.price());

		User instructor = userService.requireUser(request.instructorId());
		validateInstructorRole(instructor);

		Course course = new Course();
		course.setId(UUID.randomUUID());
		course.setTitle(request.title().trim());
		course.setDescription(normalizeDescription(request.description()));
		course.setInstructor(instructor);
		course.setPrice(FREE_PRICE);
		course.setThumbnailUrl(blankToNull(request.thumbnailUrl()));
		course.setPublished(Boolean.TRUE.equals(request.published()));

		Course saved = courseRepository.save(course);
		return courseMapper.toResponse(saved, 0L);
	}

	/**
	 * Returns a course. Unpublished courses are visible to ADMIN only;
	 * other callers receive 404 (no existence leak).
	 */
	public CourseResponse getById(UUID id) {
		Course course = requireCourse(id);
		assertCanView(course);
		long lessonCount = lessonRepository.countBySection_Course_Id(id);
		return courseMapper.toResponse(course, lessonCount);
	}

	/**
	 * Lists courses. Non-admin callers always see published courses only,
	 * regardless of the {@code published} query parameter.
	 */
	public List<CourseResponse> list(Boolean published, UUID instructorId) {
		boolean admin = currentUserIsAdmin();
		Boolean effectivePublished = admin ? published : Boolean.TRUE;

		List<Course> courses;
		if (instructorId != null) {
			courses = courseRepository.findByInstructorId(instructorId);
			if (effectivePublished != null) {
				boolean wantPublished = effectivePublished;
				courses = courses.stream()
						.filter(course -> course.isPublished() == wantPublished)
						.toList();
			}
		}
		else if (effectivePublished != null) {
			courses = courseRepository.findByPublished(effectivePublished);
		}
		else {
			courses = courseRepository.findAll();
		}

		return toResponses(courses);
	}

	@Transactional
	public CourseResponse update(UUID id, UpdateCourseRequest request) {
		Course course = requireCourse(id);

		if (request.title() != null) {
			String title = request.title().trim();
			if (title.isEmpty()) {
				throw new IllegalArgumentException("title must not be blank");
			}
			course.setTitle(title);
		}
		if (request.description() != null) {
			course.setDescription(normalizeDescription(request.description()));
		}
		if (request.price() != null) {
			validateMvpPrice(request.price());
			course.setPrice(FREE_PRICE);
		}
		if (request.published() != null) {
			course.setPublished(request.published());
		}
		if (request.thumbnailUrl() != null) {
			course.setThumbnailUrl(blankToNull(request.thumbnailUrl()));
		}

		Course saved = courseRepository.save(course);
		long lessonCount = lessonRepository.countBySection_Course_Id(saved.getId());
		return courseMapper.toResponse(saved, lessonCount);
	}

	@Transactional
	public CourseResponse publish(UUID id) {
		Course course = requireCourse(id);
		course.setPublished(true);
		return toResponseWithLessonCount(courseRepository.save(course));
	}

	/**
	 * Unpublishes a course. True archive is not supported by the schema
	 * (no archived flag / soft-delete column).
	 */
	@Transactional
	public CourseResponse unpublish(UUID id) {
		Course course = requireCourse(id);
		course.setPublished(false);
		return toResponseWithLessonCount(courseRepository.save(course));
	}

	private List<CourseResponse> toResponses(List<Course> courses) {
		if (courses.isEmpty()) {
			return List.of();
		}
		Map<UUID, Long> lessonCounts = loadLessonCounts(
				courses.stream().map(Course::getId).toList());
		return courses.stream()
				.map(course -> courseMapper.toResponse(
						course,
						lessonCounts.getOrDefault(course.getId(), 0L)))
				.toList();
	}

	private CourseResponse toResponseWithLessonCount(Course course) {
		long lessonCount = lessonRepository.countBySection_Course_Id(course.getId());
		return courseMapper.toResponse(course, lessonCount);
	}

	private Map<UUID, Long> loadLessonCounts(List<UUID> courseIds) {
		if (courseIds.isEmpty()) {
			return Map.of();
		}
		Map<UUID, Long> counts = new HashMap<>();
		for (Object[] row : lessonRepository.countLessonsGroupedByCourseId(courseIds)) {
			counts.put((UUID) row[0], ((Number) row[1]).longValue());
		}
		return counts;
	}

	private Course requireCourse(UUID id) {
		return courseRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
	}

	private void assertCanView(Course course) {
		if (!course.isPublished() && !currentUserIsAdmin()) {
			throw new ResourceNotFoundException("Course not found: " + course.getId());
		}
	}

	static boolean currentUserIsAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return false;
		}
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (ROLE_ADMIN.equals(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}

	private static void validateInstructorRole(User instructor) {
		if (instructor.getRole() != UserRole.INSTRUCTOR && instructor.getRole() != UserRole.ADMIN) {
			throw new BusinessRuleException(
					"Course instructor must have role INSTRUCTOR or ADMIN");
		}
		if (!instructor.isActive()) {
			throw new BusinessRuleException("Course instructor must be an active user");
		}
	}

	private static void validateMvpPrice(BigDecimal price) {
		if (price == null) {
			throw new BusinessRuleException("price is required");
		}
		if (price.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("price must be non-negative");
		}
		if (price.compareTo(FREE_PRICE) != 0) {
			throw new BusinessRuleException(
					"Only free courses are supported in the MVP; price must be 0");
		}
	}

	private static String normalizeDescription(String description) {
		if (description == null) {
			return null;
		}
		String trimmed = description.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
