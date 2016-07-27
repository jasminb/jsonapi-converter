package com.github.jasminb.jsonapi;

import java.util.Map;

/**
 * Models a JSON API Link object.
 */
public class Link {

    private String href;
    private Map<String, ?> meta;

	/**
	 * Creates new Link.
	 */
	public Link() {
        // Empty CTOR
    }

	/**
	 * Creates new Link.
	 *
	 * @param href {@link String} link value
	 */
	public Link(String href) {
        this.href = href;
    }

	/**
	 * Creates new Link.
	 *
	 * @param href {@link String} link value
	 * @param meta {@link Map} link meta
	 */
	public Link(String href, Map<String, ?> meta) {
        this.href = href;
        this.meta = meta;
    }

	/**
	 * Gets href.
	 *
	 * @return the href
	 */
	public String getHref() {
        return href;
    }

	/**
	 * Sets href.
	 *
	 * @param href the href
	 */
	public void setHref(String href) {
        this.href = href;
    }

	/**
	 * Gets meta.
	 *
	 * @return the meta
	 */
	public Map<String, ?> getMeta() {
        return meta;
    }

	/**
	 * Sets meta.
	 *
	 * @param meta the meta
	 */
	public void setMeta(Map<String, ?> meta) {
        this.meta = meta;
    }

	@Override
	public String toString() {
		return String.valueOf(getHref());
	}
}
