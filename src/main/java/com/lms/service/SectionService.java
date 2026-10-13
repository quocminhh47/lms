package com.lms.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.dto.section.CreateSectionRequest;
import com.lms.dto.section.SectionResponse;
import com.lms.dto.section.UpdateSectionRequest;
import com.lms.entity.Course;
import com.lms.entity.Section;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.SectionMapper;
import com.lms.repository.CourseRepository;
import com.lms.repository.SectionRepository;

@Service
@Transactional(readOnly = true)
public class SectionService {

	private final SectionRepository sectionRepository;
	private final CourseRepository courseRepository;
	private final SectionMapper sectionMapper;

	public SectionService(
			SectionRepository sectionRepository,
			CourseRepository courseRepository,
			SectionMapper sectionMapper) {
		this.sectionRepository = sectionRepository;
		this.courseRepository = courseRepository;
		this.sectionMapper = sectionMapper;
	}

	@Transactional
	public SectionResponse create(UUID courseId, CreateSectionRequest request) {
		Course course = requireCourse(courseId);

		Section section = new Section();
		section.setId(UUID.randomUUID());
		section.setTitle(request.title().trim());
		section.setCourse(course);
		section.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);

		return sectionMapper.toResponse(sectionRepository.save(section));
	}

	public List<SectionResponse> listByCourse(UUID courseId) {
		requireVisibleCourse(courseId);
		return sectionRepository.findByCourseIdOrderBySortOrderAsc(courseId).stream()
				.map(sectionMapper::toResponse)
				.toList();
	}

	public SectionResponse getById(UUID courseId, UUID sectionId) {
		Section section = requireSectionInCourse(sectionId, courseId);
		assertCourseVisible(section.getCourse());
		return sectionMapper.toResponse(section);
	}

	@Transactional
	public SectionResponse update(UUID courseId, UUID sectionId, UpdateSectionRequest request) {
		Section section = requireSectionInCourse(sectionId, courseId);

		if (request.title() != null) {
			String title = request.title().trim();
			if (title.isEmpty()) {
				throw new IllegalArgumentException("title must not be blank");
			}
			section.setTitle(title);
		}
		if (request.sortOrder() != null) {
			section.setSortOrder(request.sortOrder());
		}

		return sectionMapper.toResponse(sectionRepository.save(section));
	}

	/**
	 * Loads a section for nested lesson operations. Does not apply catalog
	 * visibility (caller must authorize / apply visibility separately).
	 */
	public Section requireSectionInCourse(UUID sectionId, UUID courseId) {
		return sectionRepository.findByIdAndCourseId(sectionId, courseId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Section not found: " + sectionId + " in course " + courseId));
	}

	private Course requireCourse(UUID courseId) {
		return courseRepository.findById(courseId)
				.orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
	}

	private Course requireVisibleCourse(UUID courseId) {
		Course course = requireCourse(courseId);
		assertCourseVisible(course);
		return course;
	}

	private static void assertCourseVisible(Course course) {
		if (!course.isPublished() && !CourseService.currentUserIsAdmin()) {
			throw new ResourceNotFoundException("Course not found: " + course.getId());
		}
	}
}
