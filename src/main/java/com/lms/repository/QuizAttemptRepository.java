package com.lms.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.QuizAttempt;
import com.lms.entity.QuizAttemptStatus;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

	List<QuizAttempt> findByUserIdAndQuizId(UUID userId, UUID quizId);

	List<QuizAttempt> findByUserId(UUID userId);

	List<QuizAttempt> findByUserIdAndQuizIdAndStatus(
			UUID userId, UUID quizId, QuizAttemptStatus status);

	Optional<QuizAttempt> findFirstByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(
			UUID userId, UUID quizId, QuizAttemptStatus status);
}
