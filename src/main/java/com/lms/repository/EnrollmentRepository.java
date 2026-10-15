package com.lms.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.Enrollment;
import com.lms.entity.EnrollmentStatus;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

	@EntityGraph(attributePaths = "course")
	List<Enrollment> findByUserIdOrderByEnrolledAtDesc(UUID userId);

	List<Enrollment> findByUserId(UUID userId);

	List<Enrollment> findByCourseId(UUID courseId);

	Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

	@EntityGraph(attributePaths = "course") nbbb h
	Optional<Enrollment> findByIdAndUserId(UUID id, UUID userId);

	boolean existsByUserIdAndCourseIdAndStatusIn(
			UUID userId,
			UUID courseId,
			Collection<EnrollmentStatus> statuses);
}
