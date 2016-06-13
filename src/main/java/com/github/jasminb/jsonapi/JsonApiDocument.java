package com.github.jasminb.jsonapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON API Document wrapper.
 *
 * @param <T> the type parameter
 * @author jbegic
 */
public class JsonApiDocument<T> {
	private T data;

	/**
	 * Top level response link object.
	 */
	private Links links;

	/**
	 * A map of meta fields, keyed by the meta field name
	 */
	private Map<String, ?> meta = Collections.emptyMap();


	/**
	 * Creates new JsonApiDocument.
	 *
	 * @param data {@link T} API resource type
	 */
	public JsonApiDocument(T data) {
		this.data = data;
	}

	/**
	 * Gets resource object
	 *
	 * @return {@link T} resource object
	 */
	public T get() {
		return data;
	}

	/**
	 * Get meta data.
	 *
	 * @return {@link Map} meta
	 */
	public Map<String, ?> getMeta() {
		return meta;
	}

	/**
	 * Sets meta data.
	 *
	 * @param meta {@link Map} meta
	 */
	public void setMeta(Map<String, ?> meta) {
		this.meta = new HashMap<>(meta);
	}

	/**
	 * Gets links.
	 *
	 * @return the links
	 */
	public Links getLinks() {
		return links;
	}

	/**
	 * Sets links.
	 *
	 * @param links the links
	 */
	public void setLinks(Links links) {
		this.links = links;
	}
}
