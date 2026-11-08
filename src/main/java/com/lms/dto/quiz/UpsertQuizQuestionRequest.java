package com.lms.dto.quiz;

import java.util.List;

import com.lms.entity.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpsertQuizQuestionRequest(
		@NotBlank String questionText,
		@NotNull QuestionType questionType,
		@Positive Integer scoreWeight,
		@NotNull @Valid List<UpsertQuizOptionRequest> options) {
}
