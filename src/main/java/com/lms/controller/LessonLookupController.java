package com.lms.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.lesson.LessonResponse;
import com.lms.service.LessonService;

/**
 * Direct lesson lookup with full content. Publish visibility and enrollment
 * learning-access checks are enforced in the service.
 */
@RestController
@RequestMapping("/api/lessons")
public class LessonLookupController {

	private final LessonService lessonService;

	public LessonLookupController(LessonService lessonService) {
		this.lessonService = lessonService;
	}

	@GetMapping("/{lessonId}")
	public LessonResponse getById(@PathVariable UUID lessonId) {
		return lessonService.getById(lessonId);
	}
}
