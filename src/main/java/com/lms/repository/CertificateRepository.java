package com.lms.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.Certificate;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

	List<Certificate> findByUserId(UUID userId);

	List<Certificate> findByCourseId(UUID courseId);

	Optional<Certificate> findByUserIdAndCourseId(UUID userId, UUID courseId);

	Optional<Certificate> findByCertificateCode(String certificateCode);
}
