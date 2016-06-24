package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import com.github.jasminb.jsonapi.models.errors.Error;
import org.junit.Assert;
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
		Assert.assertFalse(ValidationUtils.isCollection(node));
		ValidationUtils.ensureCollection(node);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExpectObject() throws IOException {
		JsonNode node = mapper.readTree(IOUtils.getResourceAsString("users.json"));
		Assert.assertFalse(ValidationUtils.isObject(node));
		ValidationUtils.ensureObject(node);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExpectData() throws IOException {
		JsonNode node = mapper.readTree("{}");
		Assert.assertFalse(ValidationUtils.isCollection(node));
		ValidationUtils.ensureCollection(node);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDataNodeMustBeAnObject() throws IOException {
		JsonNode node = mapper.readTree("{\"data\" : \"attribute\"}");
		Assert.assertFalse(ValidationUtils.isCollection(node));
		ValidationUtils.ensureCollection(node);
	}

	@Test(expected = ResourceParseException.class)
	public void testNodeIsError() throws IOException {
		JsonNode node = mapper.readTree(IOUtils.getResourceAsString("errors.json"));
		Assert.assertTrue(ErrorUtils.hasErrors(node));
		ValidationUtils.ensureNotError(node);
	}

	@Test
	public void testRelationshipValidationPositive() throws IOException {
		Assert.assertTrue(ValidationUtils.isRelationshipParsable(mapper.readTree("{\"type\" : \"type\", \"id\" : " +
				"\"id\"}")));
	}

	@Test
	public void testRelationshipValidationNoId() throws IOException {
		Assert.assertFalse(ValidationUtils.isRelationshipParsable(mapper.readTree("{\"type\" : \"type\"}")));
	}

	@Test
	public void testRelationshipValidationNoType() throws IOException {
		Assert.assertFalse(ValidationUtils.isRelationshipParsable(mapper.readTree("{ \"id\" : \"id\"}")));
	}

	@Test
	public void testRelationshipValidationEmpty() throws IOException {
		Assert.assertFalse(ValidationUtils.isRelationshipParsable(mapper.readTree("{}")));
	}

	@Test
	public void testRelationshipValidationNull() {
		Assert.assertFalse(ValidationUtils.isRelationshipParsable(null));
	}

	@Test
	public void testRelationshipValidationNullNode() {
		Assert.assertFalse(ValidationUtils.isRelationshipParsable(NullNode.getInstance()));
	}
}
