package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.exceptions.InvalidJsonApiResourceException;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import com.github.jasminb.jsonapi.models.errors.Errors;

/**
 * Utility methods for validating segments of JSON API resource object.
 *
 * @author jbegic
 */
public class ValidationUtils {

	private ValidationUtils() {
		// Private CTOR
	}

	/**
	 * Asserts that provided resource has required 'data' or 'meta' node.
	 * @param resource resource
	 */
	public static void ensureValidResource(JsonNode resource) {
		if (!resource.has(JSONAPISpecConstants.DATA) && !resource.has(JSONAPISpecConstants.META)) {
			throw new InvalidJsonApiResourceException();
		}
	}

	/**
	 * Returns <code>true</code> in case 'DATA' note has 'ID' and 'TYPE' attributes.
	 * @param dataNode relationship data node
	 * @return <code>true</code> if node has required attributes, else <code>false</code>
	 */
	public static boolean isRelationshipParsable(JsonNode dataNode) {
		return dataNode != null && dataNode.hasNonNull(JSONAPISpecConstants.ID) && dataNode.hasNonNull(JSONAPISpecConstants.TYPE) &&
				!dataNode.get(JSONAPISpecConstants.ID).isContainerNode() && !dataNode.get(JSONAPISpecConstants.TYPE).isContainerNode();
	}

	/**
	 * Ensures that provided node does not hold 'errors' attribute.
	 * @param resourceNode resource node
	 * @throws ResourceParseException
	 */
	public static void ensureNotError(ObjectMapper mapper, JsonNode resourceNode) {
		if (resourceNode != null && resourceNode.hasNonNull(JSONAPISpecConstants.ERRORS)) {
			try {
				throw new ResourceParseException(ErrorUtils.parseError(mapper, resourceNode, Errors.class));
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
