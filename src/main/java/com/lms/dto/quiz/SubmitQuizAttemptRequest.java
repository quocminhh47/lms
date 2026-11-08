package com.lms.dto.quiz;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SubmitQuizAttemptRequest(@NotNull @Valid List<SubmitQuizAnswerRequest> answers) {
}
