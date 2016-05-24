package com.github.jasminb.jsonapi;

/**
 * Class which can represent additional non-payload data of a (non-error) JSON API response.
 *
 * Currently that means 'links'.
 *
 * @param <T> Either single POJO, or List of POJOs for collection responses.
 */
public class JsonApiDocument<T> {

	private final T data;

	private final LinksData links;

	public JsonApiDocument(T data, LinksData links) {
		this.data = data;
		this.links = links;
	}

	public T getData() {
		return data;
	}

	public LinksData getLinks() {
		return links;
	}
}
