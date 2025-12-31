package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.abstraction.JsonElement;
import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.exceptions.InvalidJsonApiResourceException;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import com.github.jasminb.jsonapi.jackson.JacksonJsonProcessor;
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
	public static void ensureValidDocument(JsonElement resourceNode) {
		if (resourceNode == null || resourceNode.isNull()) {
			throw new InvalidJsonApiResourceException();
		}

		boolean hasErrors = resourceNode.hasNonNull(JSONAPISpecConstants.ERRORS);
		boolean hasData = resourceNode.has(JSONAPISpecConstants.DATA);
		boolean hasMeta = resourceNode.has(JSONAPISpecConstants.META);

		if (hasErrors) {
			throw new ResourceParseException(ErrorUtils.parseError(resourceNode, Errors.class));
		}
		if (!hasData && !hasMeta) {
			throw new InvalidJsonApiResourceException();
		}
	}

	/**
	 * Ensures 'DATA' node is Array containing only valid Resource Objects or Resource Identifier Objects.
	 *
	 * @param dataNode array data node
	 * @throws InvalidJsonApiResourceException is thrown when 'DATA' node is not an array of valid resource objects, an array of valid resource
	 * identifier objects, or an empty array.
	 */
	public static void ensurePrimaryDataValidArray(JsonElement dataNode) {
		if (!isArrayOfResourceObjects(dataNode) && !isArrayOfResourceIdentifierObjects(dataNode)) {
			throw new InvalidJsonApiResourceException("Primary data must be an array of resource objects, an array of resource identifier objects, or an empty array ([])");
		}
	}

	/**
	 * Ensures 'DATA' node is a valid object, null or has JsonElement type NULL.
	 *
	 * @param dataNode data node.
	 * @throws InvalidJsonApiResourceException is thrown when 'DATA' node is not valid object, null or null node.
	 */
	public static void ensurePrimaryDataValidObjectOrNull(JsonElement dataNode) {
		if (!isValidObject(dataNode) && isNotNullNode(dataNode)) {
			throw new InvalidJsonApiResourceException("Primary data must be either a single resource object, a single resource identifier object, or null");
		}
	}

	/**
	 * Ensures 'DATA' node is Array containing only valid Resource Objects.
	 *
	 * @param dataNode resource object array data node
	 * @throws InvalidJsonApiResourceException is thrown when 'DATA' node is not an array of valid resource objects, or an empty array.
	 */
	public static void ensureValidResourceObjectArray(JsonElement dataNode) {
		if (!isArrayOfResourceObjects(dataNode)) {
			throw new InvalidJsonApiResourceException("Included must be an array of valid resource objects, or an empty array ([])");
		}
	}

	/**
	 * Returns  <code>true</code> in case 'DATA' node is not null and does not have JsonElement type NULL.
	 *
	 * @param dataNode data node.
	 * @return <code>false</code> if node is null or is null node <code>true</code>
	 * node.
	 */
	public static boolean isNotNullNode(JsonElement dataNode) {
		return dataNode != null && !dataNode.isNull();
	}

	/**
	 * Returns <code>true</code> in case 'DATA' node is valid Resource Object or Resource Identifier Object.
	 *
	 * @param dataNode object data node
	 * @return <code>true</code> if node is valid primary data object, else <code>false</code>
	 */
	public static boolean isValidObject(JsonElement dataNode) {
		return isResourceObject(dataNode) || isResourceIdentifierObject(dataNode);
	}

	/**
	 * Returns <code>true</code> in case node has 'ID' and 'TYPE' attributes.
	 *
	 * @param dataNode resource identifier object data node
	 * @return <code>true</code> if node has required attributes and all provided attributes are valid, else <code>false</code>
	 */
	public static boolean isResourceIdentifierObject(JsonElement dataNode) {
		return dataNode != null && dataNode.isObject() &&
				(hasValueNode(dataNode, JSONAPISpecConstants.ID) || hasValueNode(dataNode, JSONAPISpecConstants.LOCAL_ID)) &&
				hasValueNode(dataNode, JSONAPISpecConstants.TYPE) &&
				hasContainerOrNull(dataNode, JSONAPISpecConstants.META);
	}

	/**
	 * Returns <code>true</code> in case 'DATA' node has 'ATTRIBUTES' and 'TYPE' attributes.
	 *
	 * @param dataNode resource object data node
	 * @return <code>true</code> if node has required attributes and all provided attributes are valid, else <code>false</code>
	 */
	public static boolean isResourceObject(JsonElement dataNode) {
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
	 * @param dataNode resource object array data node
	 * @return <code>true</code> if node is empty array or contains only valid Resource Objects
	 */
	public static boolean isArrayOfResourceObjects(JsonElement dataNode) {
		if (dataNode != null && dataNode.isArray()) {
			for (JsonElement element : dataNode) {
				if (!isResourceObject(element) && !isResourceIdentifierObject(element)) {
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
	 * @param dataNode resource identifier object array data node
	 * @return <code>true</code> if node is empty array or contains only valid Resource Identifier Objects
	 */
	public static boolean isArrayOfResourceIdentifierObjects(JsonElement dataNode) {
		if (dataNode != null && dataNode.isArray()) {
			for (JsonElement element : dataNode) {
				if (!isResourceIdentifierObject(element)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private static boolean hasContainerNode(JsonElement dataNode, String attribute) {
		return dataNode.hasNonNull(attribute) && dataNode.get(attribute).isContainerNode();
	}

	private static boolean hasValueNode(JsonElement dataNode, String attribute) {
		return dataNode.hasNonNull(attribute) && dataNode.get(attribute).isValueNode();
	}

	private static boolean hasContainerOrNull(JsonElement dataNode, String attribute) {
		if (dataNode.hasNonNull(attribute)) {
			return dataNode.get(attribute).isContainerNode();
		}
		return true;
	}

	private static boolean hasValueOrNull(JsonElement dataNode, String attribute) {
		if (dataNode.hasNonNull(attribute)) {
			return dataNode.get(attribute).isValueNode();
		}
		return true;
	}

	// ============= BACKWARD COMPATIBILITY METHODS (DEPRECATED) =============

	/**
	 * Ensures document has at least one of 'DATA', 'ERRORS' or 'META' attributes.
	 * @deprecated Use {@link #ensureValidDocument(JsonElement)} instead
	 */
	@Deprecated
	public static void ensureValidDocument(ObjectMapper mapper, JsonNode resourceNode) {
		ensureValidDocument(JacksonJsonProcessor.wrapNode(resourceNode));
	}

	/**
	 * @deprecated Use {@link #ensurePrimaryDataValidArray(JsonElement)} instead
	 */
	@Deprecated
	public static void ensurePrimaryDataValidArray(JsonNode dataNode) {
		ensurePrimaryDataValidArray(JacksonJsonProcessor.wrapNode(dataNode));
	}

	/**
	 * @deprecated Use {@link #ensurePrimaryDataValidObjectOrNull(JsonElement)} instead
	 */
	@Deprecated
	public static void ensurePrimaryDataValidObjectOrNull(JsonNode dataNode) {
		ensurePrimaryDataValidObjectOrNull(JacksonJsonProcessor.wrapNode(dataNode));
	}

	/**
	 * @deprecated Use {@link #ensureValidResourceObjectArray(JsonElement)} instead
	 */
	@Deprecated
	public static void ensureValidResourceObjectArray(JsonNode dataNode) {
		ensureValidResourceObjectArray(JacksonJsonProcessor.wrapNode(dataNode));
	}

	/**
	 * @deprecated Use {@link #isNotNullNode(JsonElement)} instead
	 */
	@Deprecated
	public static boolean isNotNullNode(JsonNode dataNode) {
		return isNotNullNode(JacksonJsonProcessor.wrapNode(dataNode));
	}

	/**
	 * @deprecated Use {@link #isValidObject(JsonElement)} instead
	 */
	@Deprecated
	public static boolean isValidObject(JsonNode dataNode) {
		return isValidObject(JacksonJsonProcessor.wrapNode(dataNode));
	}

	/**
	 * @deprecated Use {@link #isResourceIdentifierObject(JsonElement)} instead
	 */
	@Deprecated
	public static boolean isResourceIdentifierObject(JsonNode dataNode) {
		return isResourceIdentifierObject(JacksonJsonProcessor.wrapNode(dataNode));
	}

	/**
	 * @deprecated Use {@link #isResourceObject(JsonElement)} instead
	 */
	@Deprecated
	public static boolean isResourceObject(JsonNode dataNode) {
		return isResourceObject(JacksonJsonProcessor.wrapNode(dataNode));
	}

	/**
	 * @deprecated Use {@link #isArrayOfResourceObjects(JsonElement)} instead
	 */
	@Deprecated
	public static boolean isArrayOfResourceObjects(JsonNode dataNode) {
		return isArrayOfResourceObjects(JacksonJsonProcessor.wrapNode(dataNode));
	}

	/**
	 * @deprecated Use {@link #isArrayOfResourceIdentifierObjects(JsonElement)} instead
	 */
	@Deprecated
	public static boolean isArrayOfResourceIdentifierObjects(JsonNode dataNode) {
		return isArrayOfResourceIdentifierObjects(JacksonJsonProcessor.wrapNode(dataNode));
	}

}
