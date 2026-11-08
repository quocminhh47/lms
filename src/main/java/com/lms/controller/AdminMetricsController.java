package com.lms.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.admin.CourseMetricsResponse;
import com.lms.dto.admin.StudentMetricsResponse;
import com.lms.dto.admin.UserMetricsResponse;
import com.lms.service.AdminMetricsService;

@RestController
@RequestMapping("/api/admin/metrics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMetricsController {

	private final AdminMetricsService adminMetricsService;

	public AdminMetricsController(AdminMetricsService adminMetricsService) {
		this.adminMetricsService = adminMetricsService;
	}

	@GetMapping("/courses")
	public CourseMetricsResponse courseMetrics() {
		return adminMetricsService.courseMetrics();
	}

	@GetMapping("/students")
	public StudentMetricsResponse studentMetrics() {
		return adminMetricsService.studentMetrics();
	}

	@GetMapping("/users")
	public UserMetricsResponse userMetrics() {
		return adminMetricsService.userMetrics();
	}
}
