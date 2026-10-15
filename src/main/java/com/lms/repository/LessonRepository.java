package com.lms.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lms.entity.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

	List<Lesson> findBySectionIdOrderBySortOrderAsc(UUID sectionId);

	Optional<Lesson> findByIdAndSectionId(UUID id, UUID sectionId);

	long countBySection_Course_Id(UUID courseId);

	@Query("""
			SELECT s.course.id, COUNT(l)
			FROM Lesson l
			JOIN l.section s
			WHERE s.course.id IN :courseIds
			GROUP BY s.course.id
			""")
	List<Object[]> countLessonsGroupedByCourseId(@Param("courseIds") Collection<UUID> courseIds);
}
