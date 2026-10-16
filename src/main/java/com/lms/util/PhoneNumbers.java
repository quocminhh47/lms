package com.lms.util;

/**
 * Phone number helpers. Numbers are stored in a canonical form: an optional
 * leading '+' followed by digits only. Input may contain spaces, dots, dashes
 * and parentheses; 8 to 15 digits are accepted (E.164 allows at most 15).
 */
public final class PhoneNumbers {

	/** Bean Validation pattern (also mirrored by the frontend). */
	public static final String PATTERN = "^(?=(?:\\D*\\d){8,15}\\D*$)\\+?[0-9\\s().-]+$";

	public static final String PATTERN_MESSAGE = "must be a valid phone number (8-15 digits)";

	private PhoneNumbers() {
	}

	/** Returns the canonical form, or null when the value is null or blank. */
	public static String normalize(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		String digits = trimmed.replaceAll("\\D", "");
		if (digits.isEmpty()) {
			return null;
		}
		return trimmed.startsWith("+") ? "+" + digits : digits;
	}

	public static boolean isValid(String raw) {
		return raw != null && raw.matches(PATTERN);
	}
}
