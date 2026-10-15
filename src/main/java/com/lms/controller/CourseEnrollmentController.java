package com.lms.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.enrollment.EnrollmentResponse;
import com.lms.exception.UnauthorizedException;
import com.lms.security.LmsUserPrincipal;
import com.lms.service.EnrollmentService;

/**
 * Nested enrollment creation under a course. The enrolling user is always the
 * authenticated principal — never taken from the request body.
 */
@RestController
@RequestMapping("/api/courses/{courseId}/enrollments")
public class CourseEnrollmentController {

	private final EnrollmentService enrollmentService;

	public CourseEnrollmentController(EnrollmentService enrollmentService) {
		this.enrollmentService = enrollmentService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EnrollmentResponse enroll(
			@PathVariable UUID courseId,
			@AuthenticationPrincipal LmsUserPrincipal principal) {
		requirePrincipal(principal);
		return enrollmentService.enroll(courseId, principal.getId());
	}

	private static void requirePrincipal(LmsUserPrincipal principal) {
		if (principal == null) {
			throw new UnauthorizedException("Authentication required");
		}
	}
}
