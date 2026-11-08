package com.lms.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.quiz.QuizAdminResponse;
import com.lms.dto.quiz.UpsertQuizRequest;
import com.lms.service.QuizAdminService;

import jakarta.validation.Valid;

/**
 * Admin quiz configuration for QUIZ lessons. Path matches the frontend contract:
 * {@code /api/courses/{courseId}/lessons/{lessonId}/quiz}.
 */
@RestController
@RequestMapping("/api/courses/{courseId}/lessons/{lessonId}")
@PreAuthorize("hasRole('ADMIN')")
public class QuizAdminController {

	private final QuizAdminService quizAdminService;

	public QuizAdminController(QuizAdminService quizAdminService) {
		this.quizAdminService = quizAdminService;
	}

	@GetMapping("/quiz")
	public QuizAdminResponse getQuiz(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId) {
		return quizAdminService.getAdminQuiz(courseId, lessonId);
	}

	@PutMapping("/quiz")
	public QuizAdminResponse upsertQuiz(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@Valid @RequestBody UpsertQuizRequest request) {
		return quizAdminService.upsertAdminQuiz(courseId, lessonId, request);
	}
}
