package com.lms.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.course.CourseResponse;
import com.lms.dto.course.CreateCourseRequest;
import com.lms.dto.course.UpdateCourseRequest;
import com.lms.service.CourseService;

import jakarta.validation.Valid;

/**
 * Course catalog and admin management. Public callers may only read published
 * courses; mutations and unpublished visibility are ADMIN-only.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

	private final CourseService courseService;

	public CourseController(CourseService courseService) {
		this.courseService = courseService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public CourseResponse create(@Valid @RequestBody CreateCourseRequest request) {
		return courseService.create(request);
	}

	@GetMapping("/{id}")
	public CourseResponse getById(@PathVariable UUID id) {
		return courseService.getById(id);
	}

	@GetMapping
	public List<CourseResponse> list(
			@RequestParam(required = false) Boolean published,
			@RequestParam(required = false) UUID instructorId) {
		return courseService.list(published, instructorId);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public CourseResponse update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateCourseRequest request) {
		return courseService.update(id, request);
	}

	@PostMapping("/{id}/publish")
	@PreAuthorize("hasRole('ADMIN')")
	public CourseResponse publish(@PathVariable UUID id) {
		return courseService.publish(id);
	}

	/**
	 * Unpublishes a course. Schema has no archive/soft-delete column;
	 * this is the closest supported operation via {@code is_published}.
	 */
	@PostMapping("/{id}/unpublish")
	@PreAuthorize("hasRole('ADMIN')")
	public CourseResponse unpublish(@PathVariable UUID id) {
		return courseService.unpublish(id);
	}
}
