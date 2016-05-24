package com.github.jasminb.jsonapi;

import java.util.Map;

/**
 * Simple Map container for the moment, for type-safety and readability.
 */
public class LinksData {
	private final Map<String, LinkData> map;

	public LinksData(Map<String, LinkData> map) {
		this.map = map;
	}

	public boolean containsKey(String key) {
		return map.containsKey(key);
	}

	public LinkData getLink(String key) {
		return map.get(key);
	}
}
