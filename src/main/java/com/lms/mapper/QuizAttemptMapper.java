package com.lms.mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.lms.dto.quiz.QuizAttemptAnswerResultResponse;
import com.lms.dto.quiz.QuizAttemptResultResponse;
import com.lms.dto.quiz.QuizAttemptTakeResponse;
import com.lms.dto.quiz.QuizOptionStudentResponse;
import com.lms.dto.quiz.QuizQuestionStudentResponse;
import com.lms.entity.Question;
import com.lms.entity.QuestionOption;
import com.lms.entity.Quiz;
import com.lms.entity.QuizAttempt;
import com.lms.entity.QuizAttemptAnswer;
import com.lms.entity.QuizAttemptAnswerOption;

@Component
public class QuizAttemptMapper {

	public List<QuizQuestionStudentResponse> toStudentQuestions(
			List<Question> questions,
			Map<UUID, List<QuestionOption>> optionsByQuestionId) {
		List<QuizQuestionStudentResponse> responses = new ArrayList<>();
		for (Question question : questions) {
			List<QuestionOption> options = new ArrayList<>(
					optionsByQuestionId.getOrDefault(question.getId(), List.of()));
			options.sort(Comparator.comparingInt(QuestionOption::getSortOrder)
					.thenComparing(QuestionOption::getId));
			responses.add(new QuizQuestionStudentResponse(
					question.getId(),
					question.getQuestionText(),
					question.getQuestionType(),
					options.stream()
							.map(option -> new QuizOptionStudentResponse(option.getId(), option.getOptionText()))
							.toList()));
		}
		return responses;
	}

	public QuizAttemptTakeResponse toTakeResponse(
			QuizAttempt attempt,
			Quiz quiz,
			UUID lessonId,
			boolean finalAssessment,
			int attemptsUsed,
			List<QuizQuestionStudentResponse> questions) {
		return new QuizAttemptTakeResponse(
				attempt.getId(),
				quiz.getId(),
				lessonId,
				attempt.getStatus(),
				attempt.getStartedAt(),
				attempt.getExpiresAt(),
				java.time.OffsetDateTime.now(),
				quiz.getTimeLimitMinutes(),
				quiz.getPassingScore(),
				quiz.getMaxAttempts(),
				attemptsUsed,
				finalAssessment,
				questions);
	}

	public QuizAttemptResultResponse toResultResponse(
			QuizAttempt attempt,
			Quiz quiz,
			UUID lessonId,
			boolean finalAssessment,
			int attemptsUsed,
			java.math.BigDecimal bestScore,
			Boolean bestPassed,
			List<QuizAttemptAnswerResultResponse> answers,
			List<QuizQuestionStudentResponse> questions) {
		return new QuizAttemptResultResponse(
				attempt.getId(),
				quiz.getId(),
				lessonId,
				attempt.getStatus(),
				attempt.getScore(),
				Boolean.TRUE.equals(attempt.getPassed()),
				quiz.getPassingScore(),
				attempt.getStartedAt(),
				attempt.getSubmittedAt(),
				finalAssessment,
				bestScore,
				bestPassed,
				quiz.getMaxAttempts(),
				attemptsUsed,
				answers,
				questions);
	}

	public List<QuizAttemptAnswerResultResponse> toAnswerResults(
			List<QuizAttemptAnswer> answers,
			List<QuizAttemptAnswerOption> answerOptions) {
		Map<UUID, List<UUID>> selectedByAnswerId = answerOptions.stream()
				.collect(java.util.stream.Collectors.groupingBy(
						option -> option.getAttemptAnswer().getId(),
						java.util.stream.Collectors.mapping(
								option -> option.getOption().getId(),
								java.util.stream.Collectors.toList())));
		return answers.stream()
				.map(answer -> new QuizAttemptAnswerResultResponse(
						answer.getQuestion().getId(),
						selectedByAnswerId.getOrDefault(answer.getId(), List.of())))
				.toList();
	}
}
