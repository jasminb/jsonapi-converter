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
	 * Ensures document has at least one of 'DATA', 'ERRORS' or 'META' attributes.
	 *
	 * @param resourceNode resource node
	 * @throws ResourceParseException  Maps error attribute into ResourceParseException if present.
	 * @throws InvalidJsonApiResourceException is thrown when node has none of the required attributes.
	 */
	public static void ensureValidDocument(ObjectMapper mapper, JsonNode resourceNode) {
		if (resourceNode == null || resourceNode.isNull()) {
			throw new InvalidJsonApiResourceException();
		}

		boolean hasErrors = resourceNode.hasNonNull(JSONAPISpecConstants.ERRORS);
		boolean hasData = resourceNode.has(JSONAPISpecConstants.DATA);
		boolean hasMeta = resourceNode.has(JSONAPISpecConstants.META);

		if (hasErrors) {
			try {
				throw new ResourceParseException(ErrorUtils.parseError(mapper, resourceNode, Errors.class));
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}
		if (!hasData && !hasMeta) {
			throw new InvalidJsonApiResourceException();
		}
	}

	/**
	 * Returns <code>true</code> in case 'DATA' node is valid Resource Object or Resource Identifier Object.
	 *
	 * @param dataNode resource object data node
	 * @return <code>true</code> if node is valid primary data object, else <code>false</code>
	 */
	public static boolean isValidObject(JsonNode dataNode) {
		return isResourceObject(dataNode) || isResourceIdentifierObject(dataNode);
	}

	/**
	 * Returns <code>true</code> in case 'DATA' node is Array containing only valid Resource Objects or Resource Identifier Objects.
	 *
	 * @param dataNode resource object data node
	 * @return <code>true</code> if node has Array of valid primary data objects, else <code>false</code>
	 */
	public static boolean isValidArray(JsonNode dataNode) {
		return isArrayOfResourceObjects(dataNode) || isArrayOfResourceIdentifierObjects(dataNode);
	}

	/**
	 * Returns <code>true</code> in case node has 'ID' and 'TYPE' attributes.
	 *
	 * @param dataNode resource identifier object data node
	 * @return <code>true</code> if node has required attributes and all attributes are valid, else <code>false</code>
	 */
	public static boolean isResourceIdentifierObject(JsonNode dataNode) {
		return dataNode != null && dataNode.isObject() &&
				hasValueNode(dataNode, JSONAPISpecConstants.ID) &&
				hasValueNode(dataNode, JSONAPISpecConstants.TYPE) &&
				hasContainerOrNull(dataNode, JSONAPISpecConstants.META);
	}

	/**
	 * Returns <code>true</code> in case 'DATA' node has 'ATTRIBUTES' and 'TYPE' attributes.
	 *
	 * @param dataNode resource object data node
	 * @return <code>true</code> if node has required attributes and all attributes are valid, else <code>false</code>
	 */
	public static boolean isResourceObject(JsonNode dataNode) {
		return dataNode != null && dataNode.isObject() &&
				hasValueOrNull(dataNode, JSONAPISpecConstants.ID) &&
				hasValueNode(dataNode, JSONAPISpecConstants.TYPE) &&
				hasContainerOrNull(dataNode, JSONAPISpecConstants.META) &&
				hasContainerNode(dataNode, JSONAPISpecConstants.ATTRIBUTES) &&
				hasContainerOrNull(dataNode, JSONAPISpecConstants.LINKS) &&
				hasContainerOrNull(dataNode, JSONAPISpecConstants.RELATIONSHIPS);
	}

	/**
	 * Returns <code>true</code> in case 'DATA' node has array of valid Resource Objects.
	 *
	 * @param dataNode resource object data node
	 * @return <code>true</code> if node is empty array or contains only valid Resource Objects
	 */
	public static boolean isArrayOfResourceObjects(JsonNode dataNode) {
		if (dataNode != null && dataNode.isArray()) {
			for (JsonNode element : dataNode) {
				if (!isResourceObject(element)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns <code>true</code> in case 'DATA' node has array of valid Resource Identifier Objects.
	 *
	 * @param dataNode resource object data node
	 * @return <code>true</code> if node is empty array or contains only valid Resource Identifier Objects
	 */
	public static boolean isArrayOfResourceIdentifierObjects(JsonNode dataNode) {
		if (dataNode != null && dataNode.isArray()) {
			for (JsonNode element : dataNode) {
				if (!isResourceIdentifierObject(element)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private static boolean hasContainerNode(JsonNode dataNode, String attribute) {
		return dataNode.hasNonNull(attribute) && dataNode.get(attribute).isContainerNode();
	}

	private static boolean hasValueNode(JsonNode dataNode, String attribute) {
		return dataNode.hasNonNull(attribute) && dataNode.get(attribute).isValueNode();
	}

	private static boolean hasContainerOrNull(JsonNode dataNode, String attribute) {
		if (dataNode.hasNonNull(attribute)) {
			return dataNode.get(attribute).isContainerNode();
		}
		return true;
	}

	private static boolean hasValueOrNull(JsonNode dataNode, String attribute) {
		if (dataNode.hasNonNull(attribute)) {
			return dataNode.get(attribute).isValueNode();
		}
		return true;
	}

}
