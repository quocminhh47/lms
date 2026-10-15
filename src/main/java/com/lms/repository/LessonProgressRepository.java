package com.lms.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lms.entity.LessonProgress;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

	Optional<LessonProgress> findByUserIdAndLessonId(UUID userId, UUID lessonId);

	List<LessonProgress> findByUserId(UUID userId);

	@Query("""
			SELECT lp
			FROM LessonProgress lp
			JOIN lp.lesson l
			JOIN l.section s
			WHERE lp.user.id = :userId
			  AND s.course.id = :courseId
			ORDER BY l.sortOrder ASC, l.title ASC
			""")
	List<LessonProgress> findByUserIdAndCourseId(
			@Param("userId") UUID userId,
			@Param("courseId") UUID courseId);
}
