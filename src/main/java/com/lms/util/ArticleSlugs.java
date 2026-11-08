package com.lms.util;

import java.text.Normalizer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class ArticleSlugs {

	private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");
	public static final int MAX_SLUG_LENGTH = 200;

	private ArticleSlugs() {
	}

	public static String slugify(String title) {
		String normalized = Normalizer.normalize(title.trim().toLowerCase(), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		String slug = NON_SLUG.matcher(normalized).replaceAll("-");
		slug = slug.replaceAll("(^-+)|(-+$)", "");
		if (slug.isBlank()) {
			slug = "article";
		}
		return truncate(slug, MAX_SLUG_LENGTH);
	}

	public static String allocateUnique(String title, Predicate<String> slugTaken) {
		String base = slugify(title);
		if (!slugTaken.test(base)) {
			return base;
		}
		for (int suffix = 2; suffix < 10_000; suffix++) {
			String tail = "-" + suffix;
			String candidate = truncate(base, MAX_SLUG_LENGTH - tail.length()) + tail;
			if (!slugTaken.test(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Unable to allocate a unique article slug");
	}

	private static String truncate(String value, int maxLength) {
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength).replaceAll("-+$", "");
	}
}
