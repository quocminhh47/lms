package com.lms.service.article;

import org.springframework.stereotype.Service;

@Service
public class ArticleExtractionService {

	private final SafeArticleHttpFetcher httpFetcher;
	private final ArticleHtmlExtractor htmlExtractor;
	private final ArticleHtmlSanitizer htmlSanitizer;

	public ArticleExtractionService(
			SafeArticleHttpFetcher httpFetcher,
			ArticleHtmlExtractor htmlExtractor,
			ArticleHtmlSanitizer htmlSanitizer) {
		this.httpFetcher = httpFetcher;
		this.htmlExtractor = htmlExtractor;
		this.htmlSanitizer = htmlSanitizer;
	}

	public ExtractedArticleSnapshot extractFromUrl(String articleUrl) {
		SafeArticleHttpFetcher.FetchedHtml fetched = httpFetcher.fetchHtml(articleUrl);
		ArticleHtmlExtractor.ExtractedArticle extracted = htmlExtractor.extract(fetched.finalUrl(), fetched.html());
		String sanitized = htmlSanitizer.sanitize(extracted.contentHtml(), extracted.sourceUrl());
		if (sanitized.isBlank()) {
			throw new ArticleFetchException("Sanitized article content is empty");
		}
		return new ExtractedArticleSnapshot(
				extracted.sourceUrl(),
				extracted.title(),
				extracted.author(),
				extracted.publishedAt(),
				extracted.featuredImageUrl(),
				sanitized);
	}

	public record ExtractedArticleSnapshot(
			String sourceUrl,
			String extractedTitle,
			String author,
			java.time.Instant publishedAt,
			String featuredImageUrl,
			String contentHtml) {
	}
}
