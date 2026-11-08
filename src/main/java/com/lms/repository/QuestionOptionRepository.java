package com.lms.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lms.entity.QuestionOption;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, UUID> {

	List<QuestionOption> findByQuestionIdOrderBySortOrderAscIdAsc(UUID questionId);

	@Query("""
			SELECT o FROM QuestionOption o
			JOIN FETCH o.question q
			WHERE q.id IN :questionIds
			ORDER BY o.sortOrder ASC, o.id ASC
			""")
	List<QuestionOption> findByQuestionIdInOrderBySortOrderAscIdAsc(@Param("questionIds") Collection<UUID> questionIds);

	@Modifying
	@Query("DELETE FROM QuestionOption o WHERE o.question.quiz.id = :quizId")
	void deleteByQuizId(@Param("quizId") UUID quizId);
}
