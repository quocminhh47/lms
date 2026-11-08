package com.lms.service.article;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public class ArticleHtmlExtractor {

	private static final int MIN_TEXT_LENGTH = 200;

	public ExtractedArticle extract(String pageUrl, String html) {
		Document document = Jsoup.parse(html, pageUrl);
		document.select("script, style, noscript, iframe, svg, form, nav, footer, header, aside").remove();

		String title = firstNonBlank(
				metaContent(document, "property", "og:title"),
				metaContent(document, "name", "twitter:title"),
				document.title());
		String author = firstNonBlank(
				metaContent(document, "name", "author"),
				metaContent(document, "property", "article:author"));
		Instant publishedAt = parsePublishedAt(
				metaContent(document, "property", "article:published_time"),
				metaContent(document, "name", "pubdate"),
				metaContent(document, "name", "publish-date"));
		String featuredImage = firstNonBlank(
				metaContent(document, "property", "og:image"),
				metaContent(document, "name", "twitter:image"));

		Element contentRoot = locateMainContent(document);
		if (contentRoot == null) {
			throw new ArticleFetchException("Could not identify article content on this page");
		}
		contentRoot.select(
				"script, style, noscript, iframe, nav, footer, header, aside, .ad, .ads, .advertisement")
				.remove();
		String innerHtml = contentRoot.html().trim();
		String plainText = contentRoot.text().trim();
		if (plainText.length() < MIN_TEXT_LENGTH) {
			throw new ArticleFetchException("Extracted article content is too short");
		}
		if (title == null || title.isBlank()) {
			title = contentRoot.selectFirst("h1") != null
					? contentRoot.selectFirst("h1").text().trim()
					: "Untitled article";
		}
		return new ExtractedArticle(
				title.trim(),
				blankToNull(author),
				publishedAt,
				resolveAssetUrl(pageUrl, blankToNull(featuredImage)),
				innerHtml,
				pageUrl);
	}

	private static Element locateMainContent(Document document) {
		Element article = document.selectFirst("article");
		if (article != null && article.text().trim().length() >= MIN_TEXT_LENGTH) {
			return article;
		}
		Element main = document.selectFirst("main,[role=main]");
		if (main != null && main.text().trim().length() >= MIN_TEXT_LENGTH) {
			return main;
		}
		if (article != null) {
			return article;
		}
		if (main != null) {
			return main;
		}
		Elements candidates = document.body() != null ? document.body().children() : new Elements();
		Element best = null;
		int bestScore = 0;
		for (Element candidate : candidates) {
			String tag = candidate.tagName().toLowerCase();
			if ("nav".equals(tag) || "header".equals(tag) || "footer".equals(tag) || "aside".equals(tag)) {
				continue;
			}
			int score = candidate.text().trim().length() + candidate.select("p").size() * 20;
			if (score > bestScore) {
				bestScore = score;
				best = candidate;
			}
		}
		return best;
	}

	private static Instant parsePublishedAt(String... values) {
		for (String value : values) {
			if (value == null || value.isBlank()) {
				continue;
			}
			try {
				return Instant.parse(value.trim());
			} catch (DateTimeParseException ignored) {
				// try next candidate
			}
		}
		return null;
	}

	private static String metaContent(Document document, String attribute, String key) {
		Element element = document.selectFirst("meta[" + attribute + "=" + key + "]");
		if (element == null) {
			return null;
		}
		return element.attr("content");
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}
		return null;
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private static String resolveAssetUrl(String baseUrl, String assetUrl) {
		if (assetUrl == null) {
			return null;
		}
		try {
			return java.net.URI.create(baseUrl).resolve(assetUrl).toString();
		} catch (IllegalArgumentException ex) {
			return assetUrl;
		}
	}

	public record ExtractedArticle(
			String title,
			String author,
			Instant publishedAt,
			String featuredImageUrl,
			String contentHtml,
			String sourceUrl) {
	}
}
