package com.lms.service.article;

import java.net.URI;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

@Component
public class ArticleHtmlSanitizer {

	private final PolicyFactory policy;

	public ArticleHtmlSanitizer() {
		this.policy = new HtmlPolicyBuilder()
				.allowElements(
						"h1", "h2", "h3", "h4", "h5", "h6",
						"p", "ul", "ol", "li", "blockquote",
						"strong", "em", "b", "i", "a", "img",
						"figure", "figcaption", "pre", "code", "br", "hr")
				.allowAttributes("href").onElements("a")
				.allowAttributes("src", "alt", "title").onElements("img")
				.allowUrlProtocols("http", "https")
				.requireRelNofollowOnLinks()
				.toFactory();
	}

	public String sanitize(String html, String pageUrl) {
		if (html == null || html.isBlank()) {
			return "";
		}
		String sanitized = policy.sanitize(html);
		return rewriteRelativeAssetUrls(sanitized, pageUrl);
	}

	private static String rewriteRelativeAssetUrls(String html, String baseUrl) {
		org.jsoup.nodes.Document document = org.jsoup.Jsoup.parseBodyFragment(html);
		document.select("img[src]").forEach(img -> {
			String src = img.attr("src");
			String resolved = resolveUrl(baseUrl, src);
			if (resolved != null) {
				img.attr("src", resolved);
			} else {
				img.remove();
			}
		});
		document.select("a[href]").forEach(link -> {
			String href = link.attr("href");
			String resolved = resolveUrl(baseUrl, href);
			if (resolved != null) {
				link.attr("href", resolved);
			} else {
				link.removeAttr("href");
			}
		});
		return document.body().html();
	}

	private static String resolveUrl(String baseUrl, String rawUrl) {
		if (rawUrl == null || rawUrl.isBlank()) {
			return null;
		}
		String trimmed = rawUrl.trim();
		if (trimmed.startsWith("javascript:") || trimmed.startsWith("data:")) {
			return null;
		}
		try {
			URI resolved = URI.create(baseUrl).resolve(trimmed);
			String scheme = resolved.getScheme();
			if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
				return null;
			}
			return resolved.toString();
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}
}
