package com.github.jasminb.jsonapi;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.SerializationContext;

import java.io.Serializable;
import java.util.Map;

/**
 * Models a JSON API Link object.
 */
@JsonSerialize(using = Link.LinkSerializer.class)
public class Link implements Serializable {
	private static final long serialVersionUID = -6509249812347545112L;

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


	protected static class LinkSerializer extends StdSerializer<Link> {

		public LinkSerializer() {
			super(Link.class);
		}

		@Override
		public void serialize(Link link, JsonGenerator json, SerializationContext provider) {
			if (link.getMeta() != null) {
				json.writeStartObject();
				json.writeStringProperty(JSONAPISpecConstants.HREF, link.getHref());
				json.writePOJOProperty(JSONAPISpecConstants.META, link.getMeta());
				json.writeEndObject();
			} else {
				json.writeString(link.getHref());
			}
		}
	}
}
