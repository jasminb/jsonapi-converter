package com.github.jasminb.jsonapi.models.errors;

import java.util.List;

/**
 * JSON API error response.
 *
 * @author jbegic
 */
public class ErrorResponse {
	private List<Error> errors;

	public List<Error> getErrors() {
		return errors;
	}

	public void setErrors(List<Error> errors) {
		this.errors = errors;
	}

	@Override
	public String toString() {
		return "ErrorResponse{" +
				"errors=" + (errors != null ? errors : "Undefined") +
				'}';
	}
}
