package com.lms.dto.quiz;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.lms.entity.QuizAttemptStatus;

public record QuizAttemptResultResponse(
		UUID attemptId,
		UUID quizId,
		UUID lessonId,
		QuizAttemptStatus status,
		BigDecimal score,
		boolean passed,
		int passingScore,
		OffsetDateTime startedAt,
		OffsetDateTime submittedAt,
		boolean finalAssessment,
		BigDecimal bestScore,
		Boolean bestPassed,
		Integer maxAttempts,
		int attemptsUsed,
		List<QuizAttemptAnswerResultResponse> answers,
		List<QuizQuestionStudentResponse> questions) {
}
