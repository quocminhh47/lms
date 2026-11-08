package com.lms.dto.quiz;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record UpsertQuizRequest(
		@Min(0) @Max(100) int passingScore,
		@Positive Integer timeLimitMinutes,
		@Positive Integer maxAttempts,
		@Valid List<UpsertQuizQuestionRequest> questions) {
}
