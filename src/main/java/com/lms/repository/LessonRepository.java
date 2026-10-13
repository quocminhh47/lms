package com.lms.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

	List<Lesson> findBySectionIdOrderBySortOrderAsc(UUID sectionId);
}
