package com.lms.dto.quiz;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record SubmitQuizAnswerRequest(
		@NotNull UUID questionId,
		@NotNull List<UUID> selectedOptionIds) {
}
