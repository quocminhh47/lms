package com.lms.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.section.CreateSectionRequest;
import com.lms.dto.section.SectionResponse;
import com.lms.dto.section.UpdateSectionRequest;
import com.lms.service.SectionService;

import jakarta.validation.Valid;

/**
 * Sections nested under a course. Mutations are ADMIN-only. Reads follow
 * parent-course publish visibility (unpublished → ADMIN only / 404).
 * Hard deletion is omitted: lessons and downstream FKs block safe deletes.
 */
@RestController
@RequestMapping("/api/courses/{courseId}/sections")
public class SectionController {

	private final SectionService sectionService;

	public SectionController(SectionService sectionService) {
		this.sectionService = sectionService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public SectionResponse create(
			@PathVariable UUID courseId,
			@Valid @RequestBody CreateSectionRequest request) {
		return sectionService.create(courseId, request);
	}

	@GetMapping
	public List<SectionResponse> list(@PathVariable UUID courseId) {
		return sectionService.listByCourse(courseId);
	}

	@GetMapping("/{sectionId}")
	public SectionResponse getById(
			@PathVariable UUID courseId,
			@PathVariable UUID sectionId) {
		return sectionService.getById(courseId, sectionId);
	}

	@PutMapping("/{sectionId}")
	@PreAuthorize("hasRole('ADMIN')")
	public SectionResponse update(
			@PathVariable UUID courseId,
			@PathVariable UUID sectionId,
			@Valid @RequestBody UpdateSectionRequest request) {
		return sectionService.update(courseId, sectionId, request);
	}
}
