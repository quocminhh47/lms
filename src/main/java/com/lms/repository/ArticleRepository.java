package com.lms.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.entity.Article;
import com.lms.entity.ArticleStatus;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

	@EntityGraph(attributePaths = "author")
	Optional<Article> findBySlug(String slug);

	@EntityGraph(attributePaths = "author")
	Page<Article> findByAuthorId(UUID authorId, Pageable pageable);

	@EntityGraph(attributePaths = "author")
	Page<Article> findByPublishedAtNotNullAndArchivedAtIsNull(Pageable pageable);

	@EntityGraph(attributePaths = { "author", "reviewedBy" })
	Page<Article> findByStatusOrderBySubmittedAtAsc(ArticleStatus status, Pageable pageable);

	@EntityGraph(attributePaths = { "author", "reviewedBy" })
	Page<Article> findByStatus(ArticleStatus status, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = { "author", "reviewedBy" })
	Page<Article> findAll(Pageable pageable);

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, UUID id);

	@Override
	@EntityGraph(attributePaths = { "author", "reviewedBy" })
	Optional<Article> findById(UUID id);
}
