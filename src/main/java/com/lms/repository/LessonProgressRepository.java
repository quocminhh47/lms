package com.lms.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.LessonProgress;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

	Optional<LessonProgress> findByUserIdAndLessonId(UUID userId, UUID lessonId);

	List<LessonProgress> findByUserId(UUID userId);
}
