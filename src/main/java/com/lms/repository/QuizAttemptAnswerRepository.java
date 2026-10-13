package com.lms.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.QuizAttemptAnswer;

public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, UUID> {

	List<QuizAttemptAnswer> findByAttemptId(UUID attemptId);
}
