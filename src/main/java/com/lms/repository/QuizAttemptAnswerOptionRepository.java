package com.lms.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.QuizAttemptAnswerOption;
import com.lms.entity.QuizAttemptAnswerOptionId;

public interface QuizAttemptAnswerOptionRepository
		extends JpaRepository<QuizAttemptAnswerOption, QuizAttemptAnswerOptionId> {

	List<QuizAttemptAnswerOption> findByAttemptAnswerId(UUID attemptAnswerId);

	List<QuizAttemptAnswerOption> findByOptionId(UUID optionId);
}
