package com.lms.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

	List<Question> findByQuizId(UUID quizId);
}
