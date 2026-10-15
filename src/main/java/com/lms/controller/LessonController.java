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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.lesson.CreateLessonRequest;
import com.lms.dto.lesson.LessonResponse;
import com.lms.dto.lesson.UpdateLessonRequest;
import com.lms.service.LessonService;

import jakarta.validation.Valid;

/**
 * Lessons nested under course/section. Mutations are ADMIN-only. List returns
 * public summaries (no {@code contentUrl}). Get-by-id returns full content and
 * requires ACTIVE/COMPLETED enrollment (ADMIN bypass). Hard deletion is omitted
 * because {@code lesson_progress} and {@code quizzes} reference lessons without CASCADE.
 */
@RestController
@RequestMapping("/api/courses/{courseId}/sections/{sectionId}/lessons")
public class LessonController {

	private final LessonService lessonService;

	public LessonController(LessonService lessonService) {
		this.lessonService = lessonService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public LessonResponse create(
			@PathVariable UUID courseId,
			@PathVariable UUID sectionId,
			@Valid @RequestBody CreateLessonRequest request) {
		return lessonService.create(courseId, sectionId, request);
	}

	@GetMapping
	public List<LessonResponse> list(
			@PathVariable UUID courseId,
			@PathVariable UUID sectionId) {
		return lessonService.listBySection(courseId, sectionId);
	}

	@GetMapping("/{lessonId}")
	public LessonResponse getById(
			@PathVariable UUID courseId,
			@PathVariable UUID sectionId,
			@PathVariable UUID lessonId) {
		return lessonService.getById(courseId, sectionId, lessonId);
	}

	@PutMapping("/{lessonId}")
	@PreAuthorize("hasRole('ADMIN')")
	public LessonResponse update(
			@PathVariable UUID courseId,
			@PathVariable UUID sectionId,
			@PathVariable UUID lessonId,
			@Valid @RequestBody UpdateLessonRequest request) {
		return lessonService.update(courseId, sectionId, lessonId, request);
	}
}
