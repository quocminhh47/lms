package com.lms.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.progress.LessonProgressResponse;
import com.lms.dto.progress.UpsertLessonProgressRequest;
import com.lms.exception.UnauthorizedException;
import com.lms.security.LmsUserPrincipal;
import com.lms.service.LessonProgressService;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Lesson progress for an enrolled learner in a course. The learner is always
 * the authenticated principal — never taken from the request body.
 */
@RestController
@RequestMapping("/api/courses/{courseId}")
public class CourseLessonProgressController {

	private final LessonProgressService lessonProgressService;

	public CourseLessonProgressController(LessonProgressService lessonProgressService) {
		this.lessonProgressService = lessonProgressService;
	}

	@GetMapping("/progress")
	public List<LessonProgressResponse> listForCourse(
			@PathVariable UUID courseId,
			@AuthenticationPrincipal LmsUserPrincipal principal) {
		requirePrincipal(principal);
		return lessonProgressService.listForCourse(courseId, principal.getId());
	}

	@GetMapping("/lessons/{lessonId}/progress")
	public LessonProgressResponse getForLesson(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@AuthenticationPrincipal LmsUserPrincipal principal) {
		requirePrincipal(principal);
		return lessonProgressService.getForLesson(courseId, lessonId, principal.getId());
	}

	@PutMapping("/lessons/{lessonId}/progress")
	public LessonProgressResponse upsert(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@Valid @RequestBody UpsertLessonProgressRequest request,
			@AuthenticationPrincipal LmsUserPrincipal principal) {
		requirePrincipal(principal);
		return lessonProgressService.upsert(courseId, lessonId, principal.getId(), request);
	}

	@PostMapping("/lessons/{lessonId}/progress/complete")
	public LessonProgressResponse complete(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@AuthenticationPrincipal LmsUserPrincipal principal) {
		requirePrincipal(principal);
		return lessonProgressService.complete(courseId, lessonId, principal.getId());
	}

	private static void requirePrincipal(LmsUserPrincipal principal) {
		if (principal == null) {
			throw new UnauthorizedException("Authentication required");
		}
	}
}
