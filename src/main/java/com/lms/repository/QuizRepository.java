package com.lms.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

	Optional<Quiz> findByLessonId(UUID lessonId);
}
