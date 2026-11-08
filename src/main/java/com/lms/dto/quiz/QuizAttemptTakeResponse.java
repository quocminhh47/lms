package com.lms.dto.quiz;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.lms.entity.QuizAttemptStatus;

public record QuizAttemptTakeResponse(
		UUID attemptId,
		UUID quizId,
		UUID lessonId,
		QuizAttemptStatus status,
		OffsetDateTime startedAt,
		OffsetDateTime expiresAt,
		OffsetDateTime serverNow,
		Integer timeLimitMinutes,
		int passingScore,
		Integer maxAttempts,
		int attemptsUsed,
		boolean finalAssessment,
		List<QuizQuestionStudentResponse> questions) {
}
