package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.github.jasminb.jsonapi.exceptions.InvalidJsonApiResourceException;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
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
	
	@Test(expected = InvalidJsonApiResourceException.class)
	public void testExpectData() throws IOException {
		JsonNode node = mapper.readTree("{}");
		ValidationUtils.ensureValidResource(node);
	}
	
	@Test(expected = ResourceParseException.class)
	public void testNodeIsError() throws IOException {
		JsonNode node = mapper.readTree(IOUtils.getResourceAsString("errors.json"));
		ValidationUtils.ensureNotError(mapper, node);
	}

	@Test(expected = ResourceParseException.class)
	public void testNodeWithVersionIsError() throws IOException {
		JsonNode node = mapper.readTree(IOUtils.getResourceAsString("errors-with-version.json"));
		ValidationUtils.ensureNotError(mapper, node);
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

	@Test
	public void nullObjectNode() throws IOException {
		ValidationUtils.ensureValidResource(mapper.readTree("{\"data\" : null}"));
	}
	
	@Test
	public void metaResource() throws IOException {
		ValidationUtils.ensureValidResource(mapper.readTree("{\"meta\": {}}"));
	}
}
