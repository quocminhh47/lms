package com.lms.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.article.ArticleManageResponse;
import com.lms.dto.article.CreateArticleRequest;
import com.lms.dto.article.UpdateArticleRequest;
import com.lms.dto.common.PageResponse;
import com.lms.security.LmsUserPrincipal;
import com.lms.service.ArticleService;

import jakarta.validation.Valid;

/**
 * Authenticated article authoring APIs. Ownership and workflow rules live in
 * {@link ArticleService}.
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

	private final ArticleService articleService;

	public ArticleController(ArticleService articleService) {
		this.articleService = articleService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ArticleManageResponse create(@Valid @RequestBody CreateArticleRequest request) {
		return articleService.create(request);
	}

	@PutMapping("/{id}")
	public ArticleManageResponse update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateArticleRequest request) {
		return articleService.update(id, request);
	}

	@PutMapping("/{id}/draft")
	public ArticleManageResponse saveDraft(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateArticleRequest request) {
		return articleService.saveDraft(id, request);
	}

	@PostMapping("/{id}/submit")
	public ArticleManageResponse submit(@PathVariable UUID id) {
		return articleService.submitForReview(id);
	}

	@GetMapping("/my")
	public PageResponse<ArticleManageResponse> myArticles(
			@AuthenticationPrincipal LmsUserPrincipal principal,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String sort) {
		return articleService.listByAuthor(principal.getId(), page, size, sort);
	}

	@GetMapping("/{id}/manage")
	public ArticleManageResponse getManageById(@PathVariable UUID id) {
		return articleService.getManageById(id);
	}
}
