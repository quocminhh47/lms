package com.lms.service.article;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

public final class ArticleUrlSecurityValidator {

	private static final Set<String> BLOCKED_HOSTS = Set.of(
			"localhost",
			"localhost.localdomain",
			"metadata.google.internal",
			"metadata.google",
			"instance-data");

	private ArticleUrlSecurityValidator() {
	}

	public static URI validateAndNormalize(String rawUrl) {
		if (rawUrl == null || rawUrl.isBlank()) {
			throw new ArticleFetchException("Article URL is required");
		}
		URI uri;
		try {
			uri = new URI(rawUrl.trim());
		} catch (URISyntaxException ex) {
			throw new ArticleFetchException("Article URL is not valid");
		}
		if (!uri.isAbsolute()) {
			throw new ArticleFetchException("Article URL must be absolute");
		}
		String scheme = uri.getScheme();
		if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
			throw new ArticleFetchException("Only HTTP and HTTPS URLs are allowed");
		}
		if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
			throw new ArticleFetchException("URLs with credentials are not allowed");
		}
		String host = uri.getHost();
		if (host == null || host.isBlank()) {
			throw new ArticleFetchException("Article URL must include a host");
		}
		String lowerHost = host.toLowerCase(Locale.ROOT);
		if (BLOCKED_HOSTS.contains(lowerHost) || lowerHost.endsWith(".localhost")) {
			throw new ArticleFetchException("Article URL host is not allowed");
		}
		resolveAndValidateHost(lowerHost);
		return uri;
	}

	public static void resolveAndValidateHost(String host) {
		try {
			InetAddress[] addresses = InetAddress.getAllByName(host);
			if (addresses.length == 0) {
				throw new ArticleFetchException("Article URL host could not be resolved");
			}
			for (InetAddress address : addresses) {
				if (!isPublicAddress(address)) {
					throw new ArticleFetchException("Article URL resolves to a disallowed address");
				}
			}
		} catch (UnknownHostException ex) {
			throw new ArticleFetchException("Article URL host could not be resolved");
		}
	}

	static boolean isPublicAddress(InetAddress address) {
		if (address.isAnyLocalAddress()
				|| address.isLoopbackAddress()
				|| address.isLinkLocalAddress()
				|| address.isSiteLocalAddress()
				|| address.isMulticastAddress()) {
			return false;
		}
		byte[] bytes = address.getAddress();
		if (bytes.length == 4) {
			int first = bytes[0] & 0xFF;
			int second = bytes[1] & 0xFF;
			if (first == 0 || first == 10 || first == 127) {
				return false;
			}
			if (first == 100 && second >= 64 && second <= 127) {
				return false;
			}
			if (first == 169 && second == 254) {
				return false;
			}
			if (first == 172 && second >= 16 && second <= 31) {
				return false;
			}
			if (first == 192 && second == 168) {
				return false;
			}
			if (first == 192 && second == 0 && (bytes[2] & 0xFF) == 0) {
				return false;
			}
			if (first >= 224) {
				return false;
			}
		}
		if (bytes.length == 16) {
			int first = bytes[0] & 0xFF;
			if (first == 0x00 && (bytes[1] & 0xFF) == 0x00) {
				return false;
			}
			if ((first & 0xFE) == 0xFC) {
				return false;
			}
			if (first == 0xFD) {
				return false;
			}
			if (first == 0xFE && (bytes[1] & 0xC0) == 0x80) {
				return false;
			}
		}
		return true;
	}
}
