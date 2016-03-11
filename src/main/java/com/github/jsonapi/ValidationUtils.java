package com.github.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import static com.github.jsonapi.JSONAPISpecConstants.*;

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
	 * Asserts that provided resource has required 'data' node and that node is of type object.
	 * @param resource resource
	 */
	public static void ensureObject(JsonNode resource) {
		if (ensureDataNode(resource).isArray()) {
			throw new IllegalArgumentException("'data' node is not an object!");
		}
	}

	/**
	 * Returns <code>true</code> in case 'DATA' note has 'ID' and 'TYPE' attributes.
	 * @param dataNode relationship data node
	 * @return <code>true</code> if node has required attributes, else <code>false</code>
	 */
	public static boolean isRelationshipParsable(JsonNode dataNode) {
		if (dataNode != null) {
			return dataNode != null && dataNode.hasNonNull(ID) && dataNode.hasNonNull(TYPE) &&
					!dataNode.get(ID).isContainerNode() && !dataNode.get(TYPE).isContainerNode();
		}

		return false;
	}

	private static JsonNode ensureDataNode(JsonNode resource) {
		JsonNode dataNode = resource.get(DATA);

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
