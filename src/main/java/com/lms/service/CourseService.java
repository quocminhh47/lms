package com.lms.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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

@Service
@Transactional(readOnly = true)
public class CourseService {

	private static final BigDecimal FREE_PRICE = BigDecimal.ZERO;

	private final CourseRepository courseRepository;
	private final CourseMapper courseMapper;
	private final UserService userService;

	public CourseService(
			CourseRepository courseRepository,
			CourseMapper courseMapper,
			UserService userService) {
		this.courseRepository = courseRepository;
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
		course.setPublished(Boolean.TRUE.equals(request.published()));

		return courseMapper.toResponse(courseRepository.save(course));
	}

	public CourseResponse getById(UUID id) {
		return courseMapper.toResponse(requireCourse(id));
	}

	public List<CourseResponse> list(Boolean published, UUID instructorId) {
		List<Course> courses;
		if (instructorId != null) {
			courses = courseRepository.findByInstructorId(instructorId);
			if (published != null) {
				courses = courses.stream()
						.filter(course -> course.isPublished() == published)
						.toList();
			}
		}
		else if (published != null) {
			courses = courseRepository.findByPublished(published);
		}
		else {
			courses = courseRepository.findAll();
		}

		return courses.stream()
				.map(courseMapper::toResponse)
				.toList();
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

		return courseMapper.toResponse(courseRepository.save(course));
	}

	/**
	 * Unpublishes a course. True archive is not supported by the schema
	 * (no archived flag / soft-delete column).
	 */
	@Transactional
	public CourseResponse unpublish(UUID id) {
		Course course = requireCourse(id);
		course.setPublished(false);
		return courseMapper.toResponse(courseRepository.save(course));
	}

	private Course requireCourse(UUID id) {
		return courseRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
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
}
