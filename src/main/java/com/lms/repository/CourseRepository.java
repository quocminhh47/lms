package com.lms.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.Course;

public interface CourseRepository extends JpaRepository<Course, UUID> {

	@EntityGraph(attributePaths = "instructor")
	List<Course> findByInstructorId(UUID instructorId);

	@EntityGraph(attributePaths = "instructor")
	List<Course> findByPublished(boolean published);

	@Override
	@EntityGraph(attributePaths = "instructor")
	List<Course> findAll();

	Optional<Course> findByFinalQuizId(UUID finalQuizId);
}
