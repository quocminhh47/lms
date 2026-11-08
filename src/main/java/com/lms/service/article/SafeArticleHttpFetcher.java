package com.lms.service.article;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class SafeArticleHttpFetcher {

	private static final int MAX_REDIRECTS = 5;
	private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

	private final HttpClient httpClient;

	public SafeArticleHttpFetcher() {
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(CONNECT_TIMEOUT)
				.followRedirects(HttpClient.Redirect.NEVER)
				.build();
	}

	public FetchedHtml fetchHtml(String rawUrl) {
		URI current = ArticleUrlSecurityValidator.validateAndNormalize(rawUrl);
		int redirects = 0;
		while (true) {
			HttpRequest request = HttpRequest.newBuilder(current)
					.timeout(READ_TIMEOUT)
					.header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.5")
					.header("User-Agent", "LMS-ArticleReader/1.0")
					.GET()
					.build();
			HttpResponse<byte[]> response;
			try {
				response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new ArticleFetchException("Article request was interrupted");
			} catch (IOException ex) {
				throw new ArticleFetchException("Unable to retrieve article");
			}
			int status = response.statusCode();
			if (isRedirect(status)) {
				if (redirects >= MAX_REDIRECTS) {
					throw new ArticleFetchException("Too many redirects");
				}
				Optional<String> location = response.headers().firstValue("location");
				if (location.isEmpty() || location.get().isBlank()) {
					throw new ArticleFetchException("Invalid redirect response");
				}
				current = resolveRedirect(current, location.get().trim());
				redirects++;
				continue;
			}
			if (status < 200 || status >= 300) {
				throw new ArticleFetchException("Article URL returned an error response");
			}
			byte[] body = response.body();
			if (body.length > MAX_BODY_BYTES) {
				throw new ArticleFetchException("Article response is too large");
			}
			String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
			if (!contentType.isBlank()
					&& !contentType.startsWith("text/html")
					&& !contentType.startsWith("application/xhtml")) {
				throw new ArticleFetchException("Article URL did not return HTML content");
			}
			Charset charset = charsetFromContentType(contentType);
			String html = new String(body, charset);
			return new FetchedHtml(current.toString(), html);
		}
	}

	private static URI resolveRedirect(URI current, String location) {
		URI next;
		try {
			next = current.resolve(location);
		} catch (IllegalArgumentException ex) {
			throw new ArticleFetchException("Invalid redirect URL");
		}
		return ArticleUrlSecurityValidator.validateAndNormalize(next.toString());
	}

	private static boolean isRedirect(int status) {
		return status == 301
				|| status == 302
				|| status == 303
				|| status == 307
				|| status == 308;
	}

	private static Charset charsetFromContentType(String contentType) {
		int charsetIndex = contentType.indexOf("charset=");
		if (charsetIndex < 0) {
			return StandardCharsets.UTF_8;
		}
		String charset = contentType.substring(charsetIndex + "charset=".length()).trim();
		int end = charset.indexOf(';');
		if (end >= 0) {
			charset = charset.substring(0, end).trim();
		}
		if (charset.startsWith("\"") && charset.endsWith("\"") && charset.length() > 1) {
			charset = charset.substring(1, charset.length() - 1);
		}
		if (charset.isBlank()) {
			return StandardCharsets.UTF_8;
		}
		try {
			return Charset.forName(charset);
		} catch (Exception ex) {
			return StandardCharsets.UTF_8;
		}
	}

	public record FetchedHtml(String finalUrl, String html) {
	}
}
