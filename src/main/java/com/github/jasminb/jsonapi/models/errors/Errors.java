package com.github.jasminb.jsonapi.models.errors;

import com.github.jasminb.jsonapi.JsonApi;

import java.util.List;

/**
 * JSON API error response.
 *
 * @author jbegic
 */
public class Errors {
	private List<Error> errors;
	private JsonApi jsonapi;

	public List<Error> getErrors() {
		return errors;
	}

	public void setErrors(List<Error> errors) {
		this.errors = errors;
	}

	public JsonApi getJsonapi() {
		return jsonapi;
	}

	public void setJsonapi(JsonApi jsonapi) {
		this.jsonapi = jsonapi;
	}

	@Override
	public String toString() {
		return "Errors{" + "errors=" + (errors != null ? errors : "Undefined") + '}';
	}
}
