package com.lms.dto.quiz;

import java.util.UUID;

public record QuizAdminOptionResponse(
		UUID id,
		String optionText,
		boolean isCorrect) {
}
