package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * JSON API spec links object.
 *
 * @author jbegic
 */
public class Links {

	/**
	 * A map of link objects keyed by link name.
	 */
	private Map<String, Link> links;

	/**
	 * Create new Links.
	 * @param linkMap {@link Map} link data
	 */
	public Links(Map<String, Link> linkMap) {
		this.links = new HashMap<>(linkMap);
	}

	/**
	 * Convenience method for returning named link.
	 * @param linkName name of the link to return
	 * @return the link object, or {@code null} if the named link does not exist
	 */
	public Link getLink(String linkName) {
		return links.get(linkName);
	}

	/**
	 * Convenience method for returning the {@code prev} link.
	 *
	 * @return the link, or {@code null} if the named link does not exist
	 */
	@JsonIgnore
	public Link getPrevious() {
		return getLink(JSONAPISpecConstants.PREV);
	}

	/**
	 * Convenience method for returning the {@code first} link.
	 *
	 * @return the link, or {@code null} if the named link does not exist
	 */
	@JsonIgnore
	public Link getFirst() {
		return getLink(JSONAPISpecConstants.FIRST);
	}

	/**
	 * Convenience method for returning the {@code next} link.
	 *
	 * @return the link, or {@code null} if the named link does not exist
	 */
	@JsonIgnore
	public Link getNext() {
		return getLink(JSONAPISpecConstants.NEXT);
	}

	/**
	 * Convenience method for returning the {@code last} link.
	 *
	 * @return the link, or {@code null} if the named link does not exist
	 */
	@JsonIgnore
	public Link getLast() {
		return getLink(JSONAPISpecConstants.LAST);
	}

	/**
	 * Convenience method for returning the {@code self} link.
	 *
	 * @return the link, or {@code null} if the named link does not exist
	 */
	@JsonIgnore
	public Link getSelf() {
		return getLink(JSONAPISpecConstants.SELF);
	}

	/**
	 * Convenience method for returning the {@code related} link.
	 *
	 * @return the link, or {@code null} if the named link does not exist
	 */
	@JsonIgnore
	public Link getRelated() {
		return getLink(JSONAPISpecConstants.RELATED);
	}

	/**
	 * Gets all registered links.
	 * @return {@link Map} link data
	 */
	public Map<String, Link> getLinks() {
		return new HashMap<>(links);
	}

}
