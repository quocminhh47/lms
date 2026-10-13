package com.lms.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.QuestionOption;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, UUID> {

	List<QuestionOption> findByQuestionId(UUID questionId);
}
