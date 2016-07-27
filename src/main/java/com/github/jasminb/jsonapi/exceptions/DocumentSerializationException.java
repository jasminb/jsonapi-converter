package com.github.jasminb.jsonapi.exceptions;

/**
 * Thrown in case resource serialization fails.
 *
 * @author jbegic
 */
public class DocumentSerializationException extends Exception {

	/**
	 * Creates new DocumentSerializationException.
	 * @param cause {@link Throwable} exception cause
	 */
	public DocumentSerializationException(Throwable cause) {
		super(cause);
	}
}
