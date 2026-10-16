package com.lms.exception;

public class ConflictException extends RuntimeException {

	private final String field;

	public ConflictException(String message) {
		this(message, null);
	}

	/** @param field request field the conflict relates to (nullable) */
	public ConflictException(String message, String field) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
