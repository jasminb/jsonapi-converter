package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Simple Map container for the moment, for type-safety and readability.
 */
public class MetaData {
	private final Map<String, Object> map;
	private final JsonNode jsonNode;
	private final ObjectMapper objectMapper;

	public MetaData(Map<String, Object> map, JsonNode jsonNode, ObjectMapper objectMapper) {
		this.map = map;
		this.jsonNode = jsonNode;
		this.objectMapper = objectMapper;
	}

	public Object get(String key) {
		return map.get(key);
	}

	public boolean containsKey(String key) {
		return map.containsKey(key);
	}

	public <T> T as(Class<T> clazz) {
		return objectMapper.convertValue(jsonNode, clazz);
	}
}
