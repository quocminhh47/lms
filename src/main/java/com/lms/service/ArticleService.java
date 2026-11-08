package com.lms.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.dto.article.ArticleManageResponse;
import com.lms.dto.article.ArticlePublicDetailResponse;
import com.lms.dto.article.ArticlePublicSummaryResponse;
import com.lms.dto.article.CreateArticleRequest;
import com.lms.dto.article.RejectArticleRequest;
import com.lms.dto.article.UpdateArticleRequest;
import com.lms.dto.common.PageResponse;
import com.lms.entity.Article;
import com.lms.entity.ArticleStatus;
import com.lms.entity.User;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.UnauthorizedException;
import com.lms.mapper.ArticleMapper;
import com.lms.repository.ArticleRepository;
import com.lms.security.LmsUserPrincipal;
import com.lms.service.article.ArticleHtmlSanitizer;
import com.lms.util.ArticleSlugs;
import com.lms.util.PagingSupport;

@Service
@Transactional(readOnly = true)
public class ArticleService {

	private static final String CMS_SANITIZE_BASE_URL = "https://localhost";
	private static final int MAX_TITLE_LENGTH = 500;
	private static final int MAX_FEATURED_IMAGE_URL_LENGTH = 2048;

	private final ArticleRepository articleRepository;
	private final UserService userService;
	private final ArticleMapper articleMapper;
	private final ArticleHtmlSanitizer articleHtmlSanitizer;

	public ArticleService(
			ArticleRepository articleRepository,
			UserService userService,
			ArticleMapper articleMapper,
			ArticleHtmlSanitizer articleHtmlSanitizer) {
		this.articleRepository = articleRepository;
		this.userService = userService;
		this.articleMapper = articleMapper;
		this.articleHtmlSanitizer = articleHtmlSanitizer;
	}

	@Transactional
	public ArticleManageResponse create(CreateArticleRequest request) {
		UUID actorId = requireCurrentUserId();
		boolean admin = CourseService.currentUserIsAdmin();
		if (!admin && Boolean.TRUE.equals(request.publishImmediately())) {
			throw new BusinessRuleException("Only administrators may publish articles directly");
		}

		User author = userService.requireUser(actorId);
		Article article = new Article();
		article.setId(UUID.randomUUID());
		article.setAuthor(author);
		article.setSlug(allocateSlug(request.title(), article.getId()));
		article.setStatus(ArticleStatus.DRAFT);
		applyWorkingContent(article, request.title(), request.excerpt(),
				request.contentHtml(), request.featuredImageUrl());

		if (admin && Boolean.TRUE.equals(request.publishImmediately())) {
			User reviewer = userService.requireUser(actorId);
			publishSnapshot(article, reviewer, OffsetDateTime.now());
			article.setStatus(ArticleStatus.PUBLISHED);
		}

		return articleMapper.toManage(saveArticle(article));
	}

	@Transactional
	public ArticleManageResponse saveDraft(UUID id, UpdateArticleRequest request) {
		UUID actorId = requireCurrentUserId();
		Article article = requireArticle(id);
		assertCanManage(article, actorId);
		assertNotArchived(article);
		if (article.getStatus() != ArticleStatus.DRAFT && article.getStatus() != ArticleStatus.REJECTED) {
			throw new BusinessRuleException("Only draft or rejected articles can be saved as a draft");
		}
		if (Boolean.TRUE.equals(request.publishImmediately())) {
			throw new BusinessRuleException("Use update or submit to publish");
		}
		applyWorkingContent(article, request.title(), request.excerpt(),
				request.contentHtml(), request.featuredImageUrl());
		return articleMapper.toManage(articleRepository.save(article));
	}

	@Transactional
	public ArticleManageResponse update(UUID id, UpdateArticleRequest request) {
		UUID actorId = requireCurrentUserId();
		boolean admin = CourseService.currentUserIsAdmin();
		Article article = requireArticle(id);
		assertCanManage(article, actorId);
		assertNotArchived(article);

		if (!admin && Boolean.TRUE.equals(request.publishImmediately())) {
			throw new BusinessRuleException("Only administrators may publish articles directly");
		}
		if (!admin && article.getStatus() == ArticleStatus.PENDING_REVIEW) {
			throw new BusinessRuleException("Cannot edit article while pending review");
		}

		applyWorkingContent(article, request.title(), request.excerpt(),
				request.contentHtml(), request.featuredImageUrl());

		if (admin && Boolean.TRUE.equals(request.publishImmediately())) {
			User reviewer = userService.requireUser(actorId);
			publishSnapshot(article, reviewer, OffsetDateTime.now());
			article.setStatus(ArticleStatus.PUBLISHED);
			article.setArchivedAt(null);
			clearRejection(article);
		} else if (!admin && article.getStatus() == ArticleStatus.PUBLISHED) {
			article.setStatus(ArticleStatus.PENDING_REVIEW);
			article.setSubmittedAt(OffsetDateTime.now());
			clearRejection(article);
		}

		return articleMapper.toManage(articleRepository.save(article));
	}

	@Transactional
	public ArticleManageResponse submitForReview(UUID id) {
		UUID actorId = requireCurrentUserId();
		Article article = requireArticle(id);
		assertCanManage(article, actorId);
		assertNotArchived(article);

		if (article.getStatus() != ArticleStatus.DRAFT && article.getStatus() != ArticleStatus.REJECTED) {
			throw new BusinessRuleException("Only draft or rejected articles can be submitted for review");
		}
		validatePublishableContent(article.getTitle(), article.getContentHtml());

		article.setStatus(ArticleStatus.PENDING_REVIEW);
		article.setSubmittedAt(OffsetDateTime.now());
		clearRejection(article);
		return articleMapper.toManage(articleRepository.save(article));
	}

	@Transactional
	public ArticleManageResponse approve(UUID id) {
		requireAdmin();
		UUID adminId = requireCurrentUserId();
		Article article = requireArticle(id);
		assertNotArchived(article);

		if (article.getStatus() != ArticleStatus.PENDING_REVIEW) {
			throw new BusinessRuleException("Only pending articles can be approved");
		}
		validatePublishableContent(article.getTitle(), article.getContentHtml());

		User reviewer = userService.requireUser(adminId);
		OffsetDateTime now = OffsetDateTime.now();
		publishSnapshot(article, reviewer, now);
		article.setStatus(ArticleStatus.PUBLISHED);
		clearRejection(article);
		return articleMapper.toManage(articleRepository.save(article));
	}

	@Transactional
	public ArticleManageResponse publish(UUID id) {
		requireAdmin();
		UUID adminId = requireCurrentUserId();
		Article article = requireArticle(id);
		assertNotArchived(article);

		if (article.getStatus() != ArticleStatus.DRAFT && article.getStatus() != ArticleStatus.REJECTED) {
			throw new BusinessRuleException("Only draft or rejected articles can be published directly");
		}
		validatePublishableContent(article.getTitle(), article.getContentHtml());

		User reviewer = userService.requireUser(adminId);
		OffsetDateTime now = OffsetDateTime.now();
		publishSnapshot(article, reviewer, now);
		article.setStatus(ArticleStatus.PUBLISHED);
		clearRejection(article);
		return articleMapper.toManage(articleRepository.save(article));
	}

	@Transactional
	public ArticleManageResponse reject(UUID id, RejectArticleRequest request) {
		requireAdmin();
		UUID adminId = requireCurrentUserId();
		Article article = requireArticle(id);
		assertNotArchived(article);

		if (article.getStatus() != ArticleStatus.PENDING_REVIEW) {
			throw new BusinessRuleException("Only pending articles can be rejected");
		}
		String reason = requireNotBlank(request.reason(), "reason");

		User reviewer = userService.requireUser(adminId);
		OffsetDateTime now = OffsetDateTime.now();
		article.setStatus(ArticleStatus.REJECTED);
		article.setRejectionReason(reason);
		article.setReviewedBy(reviewer);
		article.setReviewedAt(now);
		article.setSubmittedAt(null);
		return articleMapper.toManage(articleRepository.save(article));
	}

	@Transactional
	public ArticleManageResponse archive(UUID id) {
		requireAdmin();
		Article article = requireArticle(id);
		if (article.getStatus() == ArticleStatus.ARCHIVED) {
			return articleMapper.toManage(article);
		}
		OffsetDateTime now = OffsetDateTime.now();
		article.setStatus(ArticleStatus.ARCHIVED);
		article.setArchivedAt(now);
		return articleMapper.toManage(articleRepository.save(article));
	}

	public PageResponse<ArticlePublicSummaryResponse> listPublic(int page, int size, String sort) {
		Sort effectiveSort = PagingSupport.parseSort(
				sort,
				Sort.by(Sort.Direction.DESC, "publishedAt"),
				"publishedAt",
				"createdAt",
				"title");
		Page<Article> result = articleRepository.findByPublishedAtNotNullAndArchivedAtIsNull(
				PagingSupport.pageable(page, size, effectiveSort));
		List<ArticlePublicSummaryResponse> content = result.getContent().stream()
				.filter(this::isPubliclyVisible)
				.map(articleMapper::toPublicSummary)
				.toList();
		return new PageResponse<>(
				content,
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages(),
				result.isFirst(),
				result.isLast());
	}

	public ArticlePublicDetailResponse getPublicBySlug(String slug) {
		Article article = articleRepository.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Article not found"));
		if (!isPubliclyVisible(article)) {
			throw new ResourceNotFoundException("Article not found");
		}
		return articleMapper.toPublicDetail(article);
	}

	public PageResponse<ArticleManageResponse> listByAuthor(UUID authorId, int page, int size, String sort) {
		UUID actorId = requireCurrentUserId();
		if (!authorId.equals(actorId) && !CourseService.currentUserIsAdmin()) {
			throw new AccessDeniedException("Not allowed to list articles for this author");
		}
		Sort effectiveSort = PagingSupport.parseSort(
				sort,
				Sort.by(Sort.Direction.DESC, "updatedAt"),
				"updatedAt",
				"createdAt",
				"title",
				"submittedAt");
		Page<Article> result = articleRepository.findByAuthorId(
				authorId,
				PagingSupport.pageable(page, size, effectiveSort));
		return PageResponse.from(result.map(articleMapper::toManage));
	}

	public PageResponse<ArticleManageResponse> listPendingReview(int page, int size) {
		requireAdmin();
		Page<Article> result = articleRepository.findByStatusOrderBySubmittedAtAsc(
				ArticleStatus.PENDING_REVIEW,
				PagingSupport.pageable(page, size, Sort.by(Sort.Direction.ASC, "submittedAt")));
		return PageResponse.from(result.map(articleMapper::toManage));
	}

	public PageResponse<ArticleManageResponse> listForAdmin(
			ArticleStatus status,
			int page,
			int size,
			String sort) {
		requireAdmin();
		Sort effectiveSort = PagingSupport.parseSort(
				sort,
				Sort.by(Sort.Direction.DESC, "updatedAt"),
				"updatedAt",
				"submittedAt",
				"createdAt",
				"title",
				"publishedAt");
		Page<Article> result = status == null
				? articleRepository.findAll(PagingSupport.pageable(page, size, effectiveSort))
				: articleRepository.findByStatus(status, PagingSupport.pageable(page, size, effectiveSort));
		return PageResponse.from(result.map(articleMapper::toManage));
	}

	public ArticleManageResponse getManageById(UUID id) {
		UUID actorId = requireCurrentUserId();
		Article article = requireArticle(id);
		if (!CourseService.currentUserIsAdmin() && !article.getAuthor().getId().equals(actorId)) {
			throw new ResourceNotFoundException("Article not found: " + id);
		}
		return articleMapper.toManage(article);
	}

	private Article saveArticle(Article article) {
		try {
			return articleRepository.save(article);
		} catch (DataIntegrityViolationException ex) {
			if (isSlugConflict(ex)) {
				article.setSlug(allocateSlug(article.getTitle(), article.getId()));
				return articleRepository.save(article);
			}
			throw ex;
		}
	}

	private String allocateSlug(String title, UUID articleId) {
		return ArticleSlugs.allocateUnique(
				title,
				candidate -> articleRepository.existsBySlugAndIdNot(candidate, articleId));
	}

	private void publishSnapshot(Article article, User reviewer, OffsetDateTime publishedAt) {
		article.setPublishedTitle(article.getTitle());
		article.setPublishedExcerpt(article.getExcerpt());
		article.setPublishedFeaturedImageUrl(article.getFeaturedImageUrl());
		article.setPublishedContentHtml(article.getContentHtml());
		article.setPublishedAt(publishedAt);
		article.setReviewedBy(reviewer);
		article.setReviewedAt(publishedAt);
	}

	private void applyWorkingContent(
			Article article,
			String title,
			String excerpt,
			String contentHtml,
			String featuredImageUrl) {
		String normalizedTitle = requireNotBlank(title, "title");
		if (normalizedTitle.length() > MAX_TITLE_LENGTH) {
			throw new BusinessRuleException("title must be at most " + MAX_TITLE_LENGTH + " characters");
		}
		String sanitized = sanitizeContent(requireNotBlank(contentHtml, "contentHtml"));
		validatePublishableContent(normalizedTitle, sanitized);

		article.setTitle(normalizedTitle);
		article.setExcerpt(blankToNull(excerpt));
		article.setFeaturedImageUrl(normalizeUrl(featuredImageUrl));
		article.setContentHtml(sanitized);
	}

	private String sanitizeContent(String rawHtml) {
		return articleHtmlSanitizer.sanitize(rawHtml, CMS_SANITIZE_BASE_URL);
	}

	private void validatePublishableContent(String title, String sanitizedHtml) {
		requireNotBlank(title, "title");
		if (Jsoup.parse(sanitizedHtml).text().isBlank()) {
			throw new BusinessRuleException("contentHtml must contain text");
		}
	}

	private void clearRejection(Article article) {
		article.setRejectionReason(null);
	}

	private boolean isPubliclyVisible(Article article) {
		return article.getPublishedAt() != null && article.getArchivedAt() == null;
	}

	private Article requireArticle(UUID id) {
		return articleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Article not found: " + id));
	}

	private void assertCanManage(Article article, UUID actorId) {
		if (CourseService.currentUserIsAdmin()) {
			return;
		}
		if (!article.getAuthor().getId().equals(actorId)) {
			throw new AccessDeniedException("Not allowed to manage this article");
		}
	}

	private void assertNotArchived(Article article) {
		if (article.getStatus() == ArticleStatus.ARCHIVED) {
			throw new BusinessRuleException("Archived articles cannot be modified");
		}
	}

	private void requireAdmin() {
		if (!CourseService.currentUserIsAdmin()) {
			throw new AccessDeniedException("Administrator access required");
		}
	}

	static UUID requireCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof LmsUserPrincipal principal)) {
			throw new UnauthorizedException("Authentication required");
		}
		return principal.getId();
	}

	private static String requireNotBlank(String value, String field) {
		if (value == null) {
			throw new BusinessRuleException(field + " must not be blank");
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			throw new BusinessRuleException(field + " must not be blank");
		}
		return trimmed;
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String normalizeUrl(String url) {
		String normalized = blankToNull(url);
		if (normalized != null && normalized.length() > MAX_FEATURED_IMAGE_URL_LENGTH) {
			throw new BusinessRuleException(
					"featuredImageUrl must be at most " + MAX_FEATURED_IMAGE_URL_LENGTH + " characters");
		}
		return normalized;
	}

	private static boolean isSlugConflict(DataIntegrityViolationException ex) {
		String text = ex.getMostSpecificCause() != null
				? ex.getMostSpecificCause().getMessage()
				: ex.getMessage();
		return text != null && text.toLowerCase().contains("uq_articles_slug");
	}
}
