package com.lms.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

	List<Enrollment> findByUserId(UUID userId);

	List<Enrollment> findByCourseId(UUID courseId);

	Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);
}
