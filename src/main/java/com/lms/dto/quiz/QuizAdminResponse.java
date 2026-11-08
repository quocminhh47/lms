package com.lms.dto.quiz;

import java.util.List;
import java.util.UUID;

public record QuizAdminResponse(
		UUID quizId,
		UUID lessonId,
		UUID courseId,
		int passingScore,
		Integer timeLimitMinutes,
		Integer maxAttempts,
		boolean finalAssessment,
		List<QuizAdminQuestionResponse> questions) {
}
