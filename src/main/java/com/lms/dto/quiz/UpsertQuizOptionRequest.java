package com.lms.dto.quiz;

import jakarta.validation.constraints.NotBlank;

public record UpsertQuizOptionRequest(
		@NotBlank String optionText,
		boolean isCorrect) {
}
