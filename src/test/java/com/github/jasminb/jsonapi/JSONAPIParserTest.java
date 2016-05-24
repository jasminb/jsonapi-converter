package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class JSONAPIParserTest {


	ObjectMapper objectMapper;
	JSONAPIParser parser;

	@Before
	public void setup() {
		objectMapper = new ObjectMapper();
		parser = new JSONAPIParser(objectMapper);
	}

	private JsonNode toJNode(String json) {
		try {
			return objectMapper.readTree(json);
		} catch (IOException e) {
			throw new Error("failed json parse:" + e.getMessage(), e);
		}
	}

	@Test
	public void testParseEmptyMeta() {
		MetaData md = parser.toMetaData(toJNode("{}"));
		Assert.assertNotNull("empty object should return MetaData", md);
	}
}
