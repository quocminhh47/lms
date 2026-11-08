package com.lms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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
import com.lms.entity.UserRole;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.mapper.ArticleMapper;
import com.lms.repository.ArticleRepository;
import com.lms.security.LmsUserPrincipal;
import com.lms.service.article.ArticleHtmlSanitizer;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

	private static final String BODY = "<p>Body content</p>";

	@Mock
	private ArticleRepository articleRepository;

	@Mock
	private UserService userService;

	private ArticleHtmlSanitizer articleHtmlSanitizer;
	private ArticleService articleService;

	@BeforeEach
	void setUp() {
		articleHtmlSanitizer = new ArticleHtmlSanitizer();
		articleService = new ArticleService(
				articleRepository,
				userService,
				new ArticleMapper(),
				articleHtmlSanitizer);
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void create_startsAsDraftForStudent() {
		User author = user(UUID.randomUUID(), UserRole.STUDENT);
		authenticate(author);

		when(userService.requireUser(author.getId())).thenReturn(author);
		when(articleRepository.existsBySlugAndIdNot(any(), any())).thenReturn(false);
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ArticleManageResponse response = articleService.create(
				new CreateArticleRequest("My Post", "intro", BODY, null, false));

		assertThat(response.status()).isEqualTo(ArticleStatus.DRAFT);
		assertThat(response.publishedContentHtml()).isNull();
	}

	@Test
	void create_rejectsNonAdminDirectPublish() {
		User author = user(UUID.randomUUID(), UserRole.INSTRUCTOR);
		authenticate(author);

		assertThatThrownBy(() -> articleService.create(
						new CreateArticleRequest("My Post", null, BODY, null, true)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Only administrators");
	}

	@Test
	void create_adminCanPublishImmediately() {
		User admin = user(UUID.randomUUID(), UserRole.ADMIN);
		authenticate(admin);

		when(userService.requireUser(admin.getId())).thenReturn(admin);
		when(articleRepository.existsBySlugAndIdNot(any(), any())).thenReturn(false);
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ArticleManageResponse response = articleService.create(
				new CreateArticleRequest("Launch Post", "intro", BODY, null, true));

		assertThat(response.status()).isEqualTo(ArticleStatus.PUBLISHED);
		assertThat(response.publishedTitle()).isEqualTo("Launch Post");
		assertThat(response.publishedContentHtml()).contains("Body content");
		assertThat(response.publishedAt()).isNotNull();
	}

	@Test
	void submit_movesDraftToPendingReview() {
		User author = user(UUID.randomUUID(), UserRole.STUDENT);
		Article article = draftArticle(author, "Draft title");
		authenticate(author);

		when(articleRepository.findById(article.getId())).thenReturn(Optional.of(article));
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ArticleManageResponse response = articleService.submitForReview(article.getId());

		assertThat(response.status()).isEqualTo(ArticleStatus.PENDING_REVIEW);
		assertThat(response.submittedAt()).isNotNull();
	}

	@Test
	void approve_copiesWorkingSnapshot() {
		User admin = user(UUID.randomUUID(), UserRole.ADMIN);
		Article article = pendingArticle(UUID.randomUUID(), "Working Title", "<p>Working body</p>");
		authenticate(admin);

		when(articleRepository.findById(article.getId())).thenReturn(Optional.of(article));
		when(userService.requireUser(admin.getId())).thenReturn(admin);
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ArticleManageResponse response = articleService.approve(article.getId());

		assertThat(response.status()).isEqualTo(ArticleStatus.PUBLISHED);
		assertThat(response.publishedTitle()).isEqualTo("Working Title");
		assertThat(response.publishedContentHtml()).contains("Working body");
		assertThat(response.reviewedById()).isEqualTo(admin.getId());
	}

	@Test
	void updatePublishedByAuthor_preservesPublicSnapshot() {
		User author = user(UUID.randomUUID(), UserRole.STUDENT);
		Article article = publishedArticle(author, "Live title", "<p>Live body</p>");
		authenticate(author);

		when(articleRepository.findById(article.getId())).thenReturn(Optional.of(article));
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ArticleManageResponse response = articleService.update(
				article.getId(),
				new UpdateArticleRequest("New title", "excerpt", "<p>New body</p>", null, false));

		assertThat(response.status()).isEqualTo(ArticleStatus.PENDING_REVIEW);
		assertThat(response.title()).isEqualTo("New title");
		assertThat(response.contentHtml()).contains("New body");
		assertThat(response.publishedTitle()).isEqualTo("Live title");
		assertThat(response.publishedContentHtml()).contains("Live body");
	}

	@Test
	void reject_pendingUpdateKeepsPreviousPublicVersion() {
		User admin = user(UUID.randomUUID(), UserRole.ADMIN);
		User author = user(UUID.randomUUID(), UserRole.STUDENT);
		Article article = publishedArticle(author, "Live title", "<p>Live body</p>");
		article.setStatus(ArticleStatus.PENDING_REVIEW);
		article.setTitle("Edited title");
		article.setContentHtml("<p>Edited body</p>");
		article.setSubmittedAt(OffsetDateTime.now());
		authenticate(admin);

		when(articleRepository.findById(article.getId())).thenReturn(Optional.of(article));
		when(userService.requireUser(admin.getId())).thenReturn(admin);
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ArticleManageResponse response = articleService.reject(
				article.getId(),
				new RejectArticleRequest("Needs more detail"));

		assertThat(response.status()).isEqualTo(ArticleStatus.REJECTED);
		assertThat(response.rejectionReason()).isEqualTo("Needs more detail");
		assertThat(response.publishedTitle()).isEqualTo("Live title");
		assertThat(response.publishedContentHtml()).contains("Live body");
	}

	@Test
	void submit_rejectsWhenCallerIsNotAuthor() {
		User author = user(UUID.randomUUID(), UserRole.STUDENT);
		User other = user(UUID.randomUUID(), UserRole.STUDENT);
		Article article = draftArticle(author, "Draft");
		authenticate(other);

		when(articleRepository.findById(article.getId())).thenReturn(Optional.of(article));

		assertThatThrownBy(() -> articleService.submitForReview(article.getId()))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void approve_rejectsNonAdmin() {
		User author = user(UUID.randomUUID(), UserRole.INSTRUCTOR);
		Article article = pendingArticle(author.getId(), "Title", BODY);
		authenticate(author);

		assertThatThrownBy(() -> articleService.approve(article.getId()))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void archive_setsArchivedAtAndHidesFromPublicList() {
		User admin = user(UUID.randomUUID(), UserRole.ADMIN);
		Article article = publishedArticle(admin, "Live", BODY);
		authenticate(admin);

		when(articleRepository.findById(article.getId())).thenReturn(Optional.of(article));
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ArticleManageResponse archived = articleService.archive(article.getId());

		assertThat(archived.status()).isEqualTo(ArticleStatus.ARCHIVED);
		assertThat(archived.archivedAt()).isNotNull();

		when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

		assertThatThrownBy(() -> articleService.getPublicBySlug(article.getSlug()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void create_assignsUniqueSlugWhenTitleCollides() {
		User author = user(UUID.randomUUID(), UserRole.STUDENT);
		authenticate(author);

		when(userService.requireUser(author.getId())).thenReturn(author);
		when(articleRepository.existsBySlugAndIdNot(eq("hello-world"), any())).thenReturn(true);
		when(articleRepository.existsBySlugAndIdNot(eq("hello-world-2"), any())).thenReturn(false);
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ArticleManageResponse response = articleService.create(
				new CreateArticleRequest("Hello World", null, BODY, null, false));

		ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
		verify(articleRepository).save(captor.capture());
		assertThat(captor.getValue().getSlug()).isEqualTo("hello-world-2");
		assertThat(response.slug()).isEqualTo("hello-world-2");
	}

	@Test
	void getPublicBySlug_servesPublishedSnapshot() {
		User author = user(UUID.randomUUID(), UserRole.STUDENT);
		Article article = publishedArticle(author, "Live", BODY);
		when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

		ArticlePublicDetailResponse response = articleService.getPublicBySlug(article.getSlug());

		assertThat(response.title()).isEqualTo("Live");
		assertThat(response.contentHtml()).contains("Body content");
	}

	@Test
	void listPublic_usesPublishedSnapshotFields() {
		User author = user(UUID.randomUUID(), UserRole.STUDENT);
		Article article = publishedArticle(author, "Live", BODY);
		when(articleRepository.findByPublishedAtNotNullAndArchivedAtIsNull(any(Pageable.class)))
				.thenReturn(new PageImpl<>(java.util.List.of(article)));

		PageResponse<ArticlePublicSummaryResponse> page = articleService.listPublic(0, 20, null);

		assertThat(page.content()).hasSize(1);
		assertThat(page.content().get(0).title()).isEqualTo("Live");
	}

	@Test
	void submit_rejectsArchivedArticle() {
		User admin = user(UUID.randomUUID(), UserRole.ADMIN);
		Article article = draftArticle(admin, "Draft");
		article.setStatus(ArticleStatus.ARCHIVED);
		authenticate(admin);

		when(articleRepository.findById(article.getId())).thenReturn(Optional.of(article));

		assertThatThrownBy(() -> articleService.submitForReview(article.getId()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Archived");
	}

	private static void authenticate(User user) {
		LmsUserPrincipal principal = new LmsUserPrincipal(user);
		var auth = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private static User user(UUID id, UserRole role) {
		User user = new User();
		user.setId(id);
		user.setEmail(id + "@example.com");
		user.setPasswordHash("hash");
		user.setFullName("Test User");
		user.setRole(role);
		user.setActive(true);
		return user;
	}

	private static Article draftArticle(User author, String title) {
		Article article = new Article();
		article.setId(UUID.randomUUID());
		article.setAuthor(author);
		article.setSlug("draft-" + article.getId());
		article.setStatus(ArticleStatus.DRAFT);
		article.setTitle(title);
		article.setContentHtml(BODY);
		return article;
	}

	private static Article pendingArticle(UUID authorId, String title, String body) {
		User author = user(authorId, UserRole.STUDENT);
		Article article = draftArticle(author, title);
		article.setContentHtml(body);
		article.setStatus(ArticleStatus.PENDING_REVIEW);
		article.setSubmittedAt(OffsetDateTime.now());
		return article;
	}

	private static Article publishedArticle(User author, String publishedTitle, String publishedBody) {
		Article article = draftArticle(author, publishedTitle);
		article.setStatus(ArticleStatus.PUBLISHED);
		article.setPublishedTitle(publishedTitle);
		article.setPublishedExcerpt("excerpt");
		article.setPublishedContentHtml(publishedBody);
		article.setPublishedAt(OffsetDateTime.now().minusDays(1));
		article.setContentHtml(publishedBody);
		return article;
	}
}
