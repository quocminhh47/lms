package com.lms.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.Section;

public interface SectionRepository extends JpaRepository<Section, UUID> {

	List<Section> findByCourseIdOrderBySortOrderAsc(UUID courseId);

	Optional<Section> findByIdAndCourseId(UUID id, UUID courseId);
}
