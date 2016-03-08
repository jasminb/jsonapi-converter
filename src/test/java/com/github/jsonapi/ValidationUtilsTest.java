package com.github.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Covering cases for validation utility class.
 *
 * @author jbegic
 */
public class ValidationUtilsTest {
	private ObjectMapper mapper;

	@Before
	public void setup() {
		mapper = new ObjectMapper();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExpectCollection() throws IOException {
		JsonNode node = mapper.readTree(IOUtils.getResourceAsString("user-with-statuses.json"));
		ValidationUtils.ensureCollection(node);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExpectObject() throws IOException {
		JsonNode node = mapper.readTree(IOUtils.getResourceAsString("users.json"));
		ValidationUtils.ensureObject(node);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExpectData() throws IOException {
		JsonNode node = mapper.readTree("{}");
		ValidationUtils.ensureCollection(node);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDataNodeMustBeAnObject() throws IOException {
		JsonNode node = mapper.readTree("{\"data\" : \"attribute\"}");
		ValidationUtils.ensureCollection(node);
	}
}
