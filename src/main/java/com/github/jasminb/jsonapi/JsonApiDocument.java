package com.github.jasminb.jsonapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON API Document wrapper.
 *
 * @author jbegic
 */
public class JsonApiDocument<T> {
	private T data;

	/**
	 * A map of link objects keyed by link name.
	 */
	private Map<String, Link> links = Collections.emptyMap();

	/**
	 * A map of meta fields, keyed by the meta field name
	 */
	private Map<String, ?> meta = Collections.emptyMap();


	/**
	 * Creates new JsonApiDocument.
	 * @param data {@link T} API resource type
	 */
	public JsonApiDocument(T data) {
		this.data = data;
	}

	/**
	 * Gets resource object
	 * @return {@link T} resource object
	 */
	public T get() {
		return data;
	}

	/**
	 * Gets all registered links.
	 * @return {@link Map} link data
	 */
	public Map<String, Link> getLinks() {
		return links;
	}

	/**
	 * Sets link data.
	 * @param links {@link Map} link data
	 */
	public void setLinks(Map<String, Link> links) {
		this.links = new HashMap<>(links);
	}

	/**
	 * Get meta data.
	 * @return {@link Map} meta
	 */
	public Map<String, ?> getMeta() {
		return meta;
	}

	/**
	 * Sets meta data.
	 * @param meta {@link Map} meta
	 */
	public void setMeta(Map<String, ?> meta) {
		this.meta = new HashMap<>(meta);
	}

	/**
	 * Convenience method for returning the value of the named link.
	 * @param linkName name of the link to return
	 * @return the link value, or {@code null} if the named link does not exist or has no value
	 */
	public String getLink(String linkName) {
		if (links.containsKey(linkName)) {
			return links.get(linkName).getHref();
		}

		return null;
	}

	/**
	 * Convenience method for returning the value of the {@code prev} link.
	 *
	 * @return the link value, or {@code null} if the named link does not exist or has no value
	 */
	public String getPrevious() {
		return getLink(JSONAPISpecConstants.PREV);
	}

	/**
	 * Convenience method for returning the value of the {@code first} link.
	 *
	 * @return the link value, or {@code null} if the named link does not exist or has no value
	 */
	public String getFirst() {
		return getLink(JSONAPISpecConstants.FIRST);
	}

	/**
	 * Convenience method for returning the value of the {@code next} link.
	 *
	 * @return the link value, or {@code null} if the named link does not exist or has no value
	 */
	public String getNext() {
		return getLink(JSONAPISpecConstants.NEXT);
	}

	/**
	 * Convenience method for returning the value of the {@code last} link.
	 *
	 * @return the link value, or {@code null} if the named link does not exist or has no value
	 */
	public String getLast() {
		return getLink(JSONAPISpecConstants.LAST);
	}

	/**
	 * Convenience method for returning the value of the {@code self} link.
	 *
	 * @return the link value, or {@code null} if the named link does not exist or has no value
	 */
	public String getSelf() {
		return getLink(JSONAPISpecConstants.SELF);
	}

	/**
	 * Convenience method for returning the value of the {@code related} link.
	 *
	 * @return the link value, or {@code null} if the named link does not exist or has no value
	 */
	public String getRelated() {
		return getLink(JSONAPISpecConstants.RELATED);
	}
}
