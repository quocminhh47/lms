package com.lms.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lms.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

	List<Question> findByQuizIdOrderBySortOrderAscIdAsc(UUID quizId);

	@Modifying
	@Query("DELETE FROM Question q WHERE q.quiz.id = :quizId")
	void deleteByQuizId(@Param("quizId") UUID quizId);
}
