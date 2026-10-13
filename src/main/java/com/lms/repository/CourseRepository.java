package com.lms.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.Course;

public interface CourseRepository extends JpaRepository<Course, UUID> {

	List<Course> findByInstructorId(UUID instructorId);

	List<Course> findByPublished(boolean published);

	Optional<Course> findByFinalQuizId(UUID finalQuizId);
}
