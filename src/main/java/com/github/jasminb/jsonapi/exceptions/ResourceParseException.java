package com.github.jasminb.jsonapi.exceptions;

import com.github.jasminb.jsonapi.models.errors.ErrorResponse;

/**
 * ResourceParseException implementation. <br />
 * This exception is thrown from ResourceConverter in case parsed body contains 'errors' node.
 *
 * @author jbegic
 */
public class ResourceParseException extends RuntimeException {
	private ErrorResponse errorResponse;

	public ResourceParseException(ErrorResponse errorResponse) {
		super(errorResponse.toString());
		this.errorResponse = errorResponse;
	}

	/**
	 * Returns ErrorResponse or <code>null</code>
	 * @return
	 */
	public ErrorResponse getErrorResponse() {
		return errorResponse;
	}
}
