package com.lms.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.enrollment.EnrollmentResponse;
import com.lms.exception.UnauthorizedException;
import com.lms.security.LmsUserPrincipal;
import com.lms.service.EnrollmentService;

/**
 * Current-user enrollment reads. Ownership is derived from the JWT principal;
 * clients cannot supply another user id.
 */
@RestController
@RequestMapping("/api/enrollments/me")
public class MyEnrollmentController {

	private final EnrollmentService enrollmentService;

	public MyEnrollmentController(EnrollmentService enrollmentService) {
		this.enrollmentService = enrollmentService;
	}

	@GetMapping
	public List<EnrollmentResponse> listMine(@AuthenticationPrincipal LmsUserPrincipal principal) {
		requirePrincipal(principal);
		return enrollmentService.listMine(principal.getId());
	}

	@GetMapping("/{enrollmentId}")
	public EnrollmentResponse getMine(
			@PathVariable UUID enrollmentId,
			@AuthenticationPrincipal LmsUserPrincipal principal) {
		requirePrincipal(principal);
		return enrollmentService.getMine(enrollmentId, principal.getId());
	}

	private static void requirePrincipal(LmsUserPrincipal principal) {
		if (principal == null) {
			throw new UnauthorizedException("Authentication required");
		}
	}
}
