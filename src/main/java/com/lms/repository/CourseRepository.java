package com.lms.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lms.entity.Course;

public interface CourseRepository extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {

	long countByPublished(boolean published);

	@Query("""
			SELECT COUNT(DISTINCT s.course.id)
			FROM com.lms.entity.Lesson l
			JOIN l.section s
			""")
	long countDistinctCoursesWithLessons();

	@Override
	@EntityGraph(attributePaths = "instructor")
	org.springframework.data.domain.Page<Course> findAll(
			org.springframework.data.jpa.domain.Specification<Course> spec,
			org.springframework.data.domain.Pageable pageable);

	@EntityGraph(attributePaths = "instructor")
	List<Course> findByInstructorId(UUID instructorId);

	@EntityGraph(attributePaths = "instructor")
	List<Course> findByPublished(boolean published);

	@Override
	@EntityGraph(attributePaths = "instructor")
	List<Course> findAll();

	Optional<Course> findByFinalQuizId(UUID finalQuizId);
}
