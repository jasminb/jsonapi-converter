package com.github.jasminb.jsonapi.models.errors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.jasminb.jsonapi.JSONAPISpecConstants;

import java.util.List;

/**
 * JSON API error response.
 *
 * @author jbegic
 */
@JsonIgnoreProperties(JSONAPISpecConstants.JSONAPI)
public class Errors {
	private List<Error> errors;

	public List<Error> getErrors() {
		return errors;
	}

	public void setErrors(List<Error> errors) {
		this.errors = errors;
	}

	@Override
	public String toString() {
		return "Errors{" +
				"errors=" + (errors != null ? errors : "Undefined") +
				'}';
	}
}
