package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JSONAPIParser {
	private final ObjectMapper objectMapper;

	public JSONAPIParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public MetaData toMetaData(JsonNode jsonNode) {
		if (jsonNode == null || jsonNode.isNull() || !jsonNode.isObject()) {
			return null;
		}
		// ???: is there a way to do this without objectMapper?
		JavaType type = objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class);

		Map<String, Object> map = objectMapper.convertValue(jsonNode, type);
		return new MetaData(map, jsonNode, objectMapper);
	}

	public LinksData toLinksData(JsonNode jsonNode) {

		if (jsonNode == null || jsonNode.isNull() || !jsonNode.isObject()) {
			return null;
		}
		Map<String, LinkData> map = new HashMap<>();

		Iterator<Map.Entry<String, JsonNode>> entryIterator = jsonNode.fields();

		while (entryIterator.hasNext()) {
			Map.Entry<String, JsonNode> entry = entryIterator.next();

			LinkData linkData = toLinkData(entry.getValue());
			if (linkData != null) {

				map.put(entry.getKey(), linkData);
			}
		}

		return new LinksData(map);
	}

	public LinkData toLinkData(JsonNode jsonNode) {
		if (jsonNode.isObject()) {
			String href = jsonNode.get(JSONAPISpecConstants.HREF).asText();
			if (href != null && !href.isEmpty()) {
				MetaData metaData = toMetaData(jsonNode.get(JSONAPISpecConstants.META));
				return new LinkData(href, metaData);
			}
		} else if (jsonNode.isTextual()) {
			return new LinkData(jsonNode.asText());
		}
		return null;
	}
}
