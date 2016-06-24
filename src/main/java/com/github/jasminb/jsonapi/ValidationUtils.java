package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;

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
	 * Asserts that provided resource has required 'data' node and that it holds an array object.
	 * @param resource resource
	 */
	public static void ensureCollection(JsonNode resource) {
		if (!ensureDataNode(resource).isArray()) {
			throw new IllegalArgumentException("'data' node is not an array!");
		}
	}

	/**
	 * Convenience method which forwards to {@link #ensureCollection(JsonNode)} in a try/catch.
	 *
	 * @param resource resource
	 * @return true if the provided resource has the required 'data' node and the 'data' node holds an array object;
	 *              {@code false} otherwise.
     */
	public static boolean isCollection(JsonNode resource) {
		try {
			ensureCollection(resource);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Asserts that provided resource has required 'data' node and that node is of type object.
	 * @param resource resource
	 */
	public static void ensureObject(JsonNode resource) {
		if (ensureDataNode(resource).isArray()) {
			throw new IllegalArgumentException("'data' node is not an object!");
		}
	}

	/**
	 * Convenience method which forwards to {@link #ensureObject(JsonNode)} in a try/catch.
	 *
	 * @param resource resource
	 * @return true if the provided resource has the required 'data' node and the 'data' node is of type object;
	 *              {@code false} otherwise.
	 */
	public static boolean isObject(JsonNode resource) {
		try {
			ensureObject(resource);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
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
	public static void ensureNotError(JsonNode resourceNode) {
		if (resourceNode != null && resourceNode.hasNonNull(JSONAPISpecConstants.ERRORS)) {
			try {
				throw new ResourceParseException(ErrorUtils.parseError(resourceNode));
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static JsonNode ensureDataNode(JsonNode resource) {
		JsonNode dataNode = resource.get(JSONAPISpecConstants.DATA);

		// Make sure data node exists
		if (dataNode == null) {
			throw new IllegalArgumentException("Object is missing 'data' node!");
		}

		// Make sure data node is not a simple attribute
		if (!dataNode.isContainerNode()) {
			throw new IllegalArgumentException("'data' node cannot be simple attribute!");
		}

		return dataNode;
	}
}
