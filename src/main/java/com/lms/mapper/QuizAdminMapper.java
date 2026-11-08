package com.lms.mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.lms.dto.quiz.QuizAdminOptionResponse;
import com.lms.dto.quiz.QuizAdminQuestionResponse;
import com.lms.dto.quiz.QuizAdminResponse;
import com.lms.entity.Question;
import com.lms.entity.QuestionOption;
import com.lms.entity.Quiz;

@Component
public class QuizAdminMapper {

	public QuizAdminResponse toResponse(
			Quiz quiz,
			UUID courseId,
			boolean finalAssessment,
			List<Question> questions,
			Map<UUID, List<QuestionOption>> optionsByQuestionId) {
		List<QuizAdminQuestionResponse> questionResponses = new ArrayList<>();
		for (Question question : questions) {
			List<QuestionOption> options = optionsByQuestionId.getOrDefault(question.getId(), List.of());
			options = new ArrayList<>(options);
			options.sort(Comparator.comparingInt(QuestionOption::getSortOrder)
					.thenComparing(QuestionOption::getId));
			questionResponses.add(new QuizAdminQuestionResponse(
					question.getId(),
					question.getQuestionText(),
					question.getQuestionType(),
					question.getScoreWeight(),
					options.stream()
							.map(option -> new QuizAdminOptionResponse(
									option.getId(),
									option.getOptionText(),
									option.isCorrect()))
							.toList()));
		}

		return new QuizAdminResponse(
				quiz.getId(),
				quiz.getLesson().getId(),
				courseId,
				quiz.getPassingScore(),
				quiz.getTimeLimitMinutes(),
				quiz.getMaxAttempts(),
				finalAssessment,
				questionResponses);
	}

	public Map<UUID, List<QuestionOption>> groupOptionsByQuestionId(List<QuestionOption> options) {
		return options.stream().collect(Collectors.groupingBy(option -> option.getQuestion().getId()));
	}
}
