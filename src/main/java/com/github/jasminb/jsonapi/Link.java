package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
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
		public void serialize(Link link, JsonGenerator json, SerializerProvider provider) throws IOException {
			if (link.getMeta() != null) {
				json.writeStartObject();
				json.writeStringField(JSONAPISpecConstants.HREF, link.getHref());
				json.writeObjectField(JSONAPISpecConstants.META, link.getMeta());
				json.writeEndObject();
			} else {
				json.writeString(link.getHref());
			}
		}
	}
}
