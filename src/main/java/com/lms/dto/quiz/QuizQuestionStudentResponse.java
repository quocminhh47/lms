package com.lms.dto.quiz;

import java.util.List;
import java.util.UUID;

import com.lms.entity.QuestionType;

public record QuizQuestionStudentResponse(
		UUID id,
		String questionText,
		QuestionType questionType,
		List<QuizOptionStudentResponse> options) {
}
