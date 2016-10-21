package com.github.jasminb.jsonapi.exceptions;

import com.github.jasminb.jsonapi.models.errors.Errors;

/**
 * ResourceParseException implementation. <br />
 * This exception is thrown from ResourceConverter in case parsed body contains 'errors' node.
 *
 * @author jbegic
 */
public class ResourceParseException extends RuntimeException {
	private final Errors errors;

	public ResourceParseException(Errors errors) {
		super(errors.toString());
		this.errors = errors;
	}

	/**
	 * Returns Errors or <code>null</code>
	 * @return {@link Errors}
	 */
	public Errors getErrors() {
		return errors;
	}
}
