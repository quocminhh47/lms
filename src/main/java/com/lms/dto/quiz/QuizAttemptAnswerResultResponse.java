package com.lms.dto.quiz;

import java.util.List;
import java.util.UUID;

public record QuizAttemptAnswerResultResponse(UUID questionId, List<UUID> selectedOptionIds) {
}
