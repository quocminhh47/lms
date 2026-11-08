package com.lms.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.article.ArticleManageResponse;
import com.lms.dto.article.RejectArticleRequest;
import com.lms.dto.common.PageResponse;
import com.lms.entity.ArticleStatus;
import com.lms.service.ArticleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/articles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminArticleController {

	private final ArticleService articleService;

	public AdminArticleController(ArticleService articleService) {
		this.articleService = articleService;
	}

	@GetMapping
	public PageResponse<ArticleManageResponse> list(
			@RequestParam(required = false) ArticleStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String sort) {
		return articleService.listForAdmin(status, page, size, sort);
	}

	@GetMapping("/pending")
	public PageResponse<ArticleManageResponse> listPending(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return articleService.listPendingReview(page, size);
	}

	@PostMapping("/{id}/publish")
	public ArticleManageResponse publish(@PathVariable UUID id) {
		return articleService.publish(id);
	}

	@PostMapping("/{id}/approve")
	public ArticleManageResponse approve(@PathVariable UUID id) {
		return articleService.approve(id);
	}

	@PostMapping("/{id}/reject")
	public ArticleManageResponse reject(
			@PathVariable UUID id,
			@Valid @RequestBody RejectArticleRequest request) {
		return articleService.reject(id, request);
	}

	@PostMapping("/{id}/archive")
	public ArticleManageResponse archive(@PathVariable UUID id) {
		return articleService.archive(id);
	}
}
