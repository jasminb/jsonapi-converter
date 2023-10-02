package com.github.jasminb.jsonapi.exceptions;

public class UnknownRelationshipException extends RuntimeException {

	public UnknownRelationshipException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnknownRelationshipException(String message) {
		super(message);
	}
}
