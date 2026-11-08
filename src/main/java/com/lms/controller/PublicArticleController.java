package com.lms.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.article.ArticlePublicDetailResponse;
import com.lms.dto.article.ArticlePublicSummaryResponse;
import com.lms.dto.common.PageResponse;
import com.lms.service.ArticleService;

/**
 * Public article catalog. Responses expose published snapshot fields only.
 */
@RestController
@RequestMapping("/api/public/articles")
public class PublicArticleController {

	private final ArticleService articleService;

	public PublicArticleController(ArticleService articleService) {
		this.articleService = articleService;
	}

	@GetMapping
	public PageResponse<ArticlePublicSummaryResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String sort) {
		return articleService.listPublic(page, size, sort);
	}

	@GetMapping("/{slug}")
	public ArticlePublicDetailResponse getBySlug(@PathVariable String slug) {
		return articleService.getPublicBySlug(slug);
	}
}
