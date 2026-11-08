package com.lms.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.quiz.QuizAttemptResultResponse;
import com.lms.dto.quiz.QuizAttemptTakeResponse;
import com.lms.dto.quiz.SubmitQuizAttemptRequest;
import com.lms.exception.UnauthorizedException;
import com.lms.security.LmsUserPrincipal;
import com.lms.service.QuizAttemptService;

import jakarta.validation.Valid;

/**
 * Student quiz attempts for QUIZ lessons. Matches the frontend contract under
 * {@code /api/courses/{courseId}/lessons/{lessonId}/quiz-attempts}.
 */
@RestController
@RequestMapping("/api/courses/{courseId}/lessons/{lessonId}/quiz-attempts")
public class QuizAttemptController {

	private final QuizAttemptService quizAttemptService;

	public QuizAttemptController(QuizAttemptService quizAttemptService) {
		this.quizAttemptService = quizAttemptService;
	}

	@GetMapping("/session")
	public Object getSession(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@AuthenticationPrincipal LmsUserPrincipal principal) {
		return quizAttemptService.getSession(courseId, lessonId, requireUserId(principal));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public QuizAttemptTakeResponse startOrResume(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@AuthenticationPrincipal LmsUserPrincipal principal) {
		return quizAttemptService.startOrResume(courseId, lessonId, requireUserId(principal));
	}

	@GetMapping("/current")
	public QuizAttemptTakeResponse getCurrent(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@AuthenticationPrincipal LmsUserPrincipal principal) {
		return quizAttemptService.getCurrent(courseId, lessonId, requireUserId(principal));
	}

	@GetMapping("/{attemptId}")
	public Object getAttempt(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@PathVariable UUID attemptId,
			@AuthenticationPrincipal LmsUserPrincipal principal) {
		return quizAttemptService.getAttempt(courseId, lessonId, attemptId, requireUserId(principal));
	}

	@PostMapping("/{attemptId}/submit")
	public QuizAttemptResultResponse submit(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@PathVariable UUID attemptId,
			@Valid @RequestBody SubmitQuizAttemptRequest request,
			@AuthenticationPrincipal LmsUserPrincipal principal) {
		return quizAttemptService.submit(courseId, lessonId, attemptId, requireUserId(principal), request);
	}

	private static UUID requireUserId(LmsUserPrincipal principal) {
		if (principal == null) {
			throw new UnauthorizedException("Authentication required");
		}
		return principal.getId();
	}
}
