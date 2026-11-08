package com.lms.dto.quiz;

import java.util.List;
import java.util.UUID;

import com.lms.entity.QuestionType;

public record QuizAdminQuestionResponse(
		UUID id,
		String questionText,
		QuestionType questionType,
		int scoreWeight,
		List<QuizAdminOptionResponse> options) {
}
