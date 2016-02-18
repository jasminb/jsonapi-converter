package com.github.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonapi.annotations.Id;
import com.github.jsonapi.annotations.Relationship;
import com.github.jsonapi.annotations.Type;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JSON API data converter. <br />
 *
 * Provides methods for conversion between JSON API resources to java POJOs and vice versa.
 *
 * @author jbegic
 */
public class ResourceConverter {
	private static final String DATA = "data";
	private static final String ATTRIBUTES = "attributes";
	private static final String TYPE = "type";
	private static final String ID = "id";
	private static final String RELATIONSHIPS = "relationships";
	private static final String INCLUDED = "included";

	private static final Map<String, Class<?>> TYPE_TO_CLASS_MAPPING = new HashMap<>();
	private static final Map<Class<?>, Type> TYPE_ANNOTATIONS = new HashMap<>();
	private static final Map<Class<?>, Field> ID_MAP = new HashMap<>();
	private static final Map<Class<?>, List<Field>> RELATIONSHIPS_MAP = new HashMap<>();

	private ObjectMapper objectMapper;

	public ResourceConverter(Class<?>... classes) {
		this(null, classes);
	}

	public ResourceConverter(ObjectMapper mapper, Class<?>... classes) {
		for (Class<?> clazz : classes) {
			if (clazz.isAnnotationPresent(Type.class)) {
				Type annotation = clazz.getAnnotation(Type.class);
				TYPE_TO_CLASS_MAPPING.put(annotation.name(), clazz);
				TYPE_ANNOTATIONS.put(clazz, annotation);

				List<Field> relationshipFields = ReflectionUtils.getAnnotatedFields(clazz, Relationship.class);

				for (Field relationshipField : relationshipFields) {
					relationshipField.setAccessible(true);
				}

				RELATIONSHIPS_MAP.put(clazz, relationshipFields);

				List<Field> idAnnotatedFields = ReflectionUtils.getAnnotatedFields(clazz, Id.class);

				if (!idAnnotatedFields.isEmpty()) {
					Field idField = idAnnotatedFields.get(0);

					if (idField != null) {
						idField.setAccessible(true);
						ID_MAP.put(clazz, idField);
					} else {
						throw new IllegalArgumentException("All resource classes must have an field annotated with the @Id annotation");
					}
				}
			} else {
				throw new IllegalArgumentException("All resource classes must be annotated with Type annotation!");
			}
		}

		// Set custom mapper if provided
		if (mapper != null) {
			objectMapper = mapper;
		} else {
			objectMapper = new ObjectMapper();
		}

		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	/**
	 * Converts raw data input into requested target type.
	 * @param data raw-data
	 * @param clazz target object
	 * @param <T>
	 * @return convrted object
	 * @throws RuntimeException in case conversion fails
	 */
	public <T> T readObject(byte [] data, Class<T> clazz) {
		try {
			JsonNode rootNode = objectMapper.readTree(data);

			JsonNode dataNode = rootNode.get(DATA);

			Map<String, Object> included = parseIncluded(rootNode);
			T result = readObject(dataNode, clazz, included);

			return result;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts raw-data input into a collection of requested output objects.
	 * @param data raw-data input
	 * @param clazz target type
	 * @param <T>
	 * @return collection of converted elements
	 * @throws RuntimeException in case conversion fails
	 */
	public <T> List<T> readObjectCollection(byte [] data, Class<T> clazz) {

		try {
			JsonNode rootNode = objectMapper.readTree(data);

			Map<String, Object> included = parseIncluded(rootNode);

			List<T> result = new ArrayList<>();

			for (JsonNode element : rootNode.get(DATA)) {
				T pojo = readObject(element, clazz, included);
				result.add(pojo);
			}

			// Populate object tree

			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}


	}

	/**
	 * Converts provided input into a target object. After conversion completes any relationships defined are resolved.
	 * @param source JSON source
	 * @param clazz target type
	 * @param cache resolved objects (either from included element or already parsed objects)
	 * @param <T>
	 * @return converted target object
	 * @throws IOException
	 * @throws IllegalAccessException
	 */
	private <T> T readObject(JsonNode source, Class<T> clazz, Map<String, Object> cache)
			throws IOException, IllegalAccessException, InstantiationException {
		T result;

		if (source.has(ATTRIBUTES)) {
			result = objectMapper.treeToValue(source.get(ATTRIBUTES), clazz);
		} else {
			result = clazz.newInstance();
		}

		// Set object id
		setIdValue(result, source.get(ID));

		if (cache != null) {
			// Handle relationships
			handleRelationships(source, result, cache);

			// Add parsed object to cache
			cache.put(createIdentifier(source), result);
		}


		return result;
	}


	/**
	 * Converts included data and returns it as pairs of its unique identifiers and converted types.
	 * @param parent data source
	 * @return identifier/object pairs
	 * @throws IOException
	 * @throws IllegalAccessException
	 */
	private Map<String, Object> parseIncluded(JsonNode parent)
			throws IOException, IllegalAccessException, InstantiationException {
		Map<String, Object> result = new HashMap<>();

		if (parent.has("included")) {
			for (JsonNode jsonNode : parent.get(INCLUDED)) {
				String type = jsonNode.get(TYPE).asText();

				if (type != null) {
					Class<?> clazz = TYPE_TO_CLASS_MAPPING.get(type);

					if (clazz != null) {
						Object object = readObject(jsonNode, clazz, null);
						result.put(createIdentifier(jsonNode), object);
					}
				}
			}
		}

		return result;
	}

	private void handleRelationships(JsonNode source, Object object, Map<String, Object> includedData)
			throws IllegalAccessException, IOException, InstantiationException {
		JsonNode relationships = source.get(RELATIONSHIPS);

		if (relationships != null) {
			Iterator<String> fields = relationships.fieldNames();

			while (fields.hasNext()) {
				String field = fields.next();

				JsonNode relationship = relationships.get(field);
				Field relationshipField = ReflectionUtils.getRelationshipField(object.getClass(), field);

				if (relationshipField != null) {
					// Make sure field is writeable
					if (!relationshipField.isAccessible()) {
						relationshipField.setAccessible(true);
					}


					if (isCollection(relationship)) {
						@SuppressWarnings("rawtypes")
						List elements = new ArrayList<>();

						for (JsonNode element : relationship.get(DATA)) {
							Class<?> type = TYPE_TO_CLASS_MAPPING.get(element.get(TYPE).asText());

							if (type != null) {
								String identifier = createIdentifier(element);
								if (includedData.containsKey(identifier)) {
									elements.add(includedData.get(identifier));
								} else {
									Object relationshipObject = readObject(element, relationshipField.getType(),
											includedData);
									elements.add(relationshipObject);
								}
							}
						}
						relationshipField.set(object, elements);

					} else {
						Class<?> type = TYPE_TO_CLASS_MAPPING.get(relationship.get(DATA).get(TYPE).asText());

						if (type != null) {
							JsonNode dataObject = relationship.get(DATA);

							String identifier = createIdentifier(dataObject);

							if (includedData.containsKey(identifier)) {
								relationshipField.set(object, includedData.get(identifier));
							} else {
								Object relationshipObject = readObject(dataObject, relationshipField.getType(), includedData);
								relationshipField.set(object, relationshipObject);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Generates unique resource identifier by combining resource type and resource id fields. <br />
	 * By specification id/type combination guarantees uniqueness.
	 * @param object data object
	 * @return concatenated id and type values
	 */
	private String createIdentifier(JsonNode object) {
		Object id = object.get(ID).asText();
		String type = object.get(TYPE).asText();
		return type.concat(id.toString());
	}

	/**
	 * Sets an id attribute value to a target object.
	 * @param target target POJO
	 * @param idValue id node
	 * @throws IllegalAccessException thrown in case target field is not accessible
	 */
	private void setIdValue(Object target, JsonNode idValue) throws IllegalAccessException {
		Field idField = ID_MAP.get(target.getClass());

		// By specification, id value is always a String type
		idField.set(target, idValue.asText());
	}

	/**
	 * Checks if <code>data</code> object is an array or just single object holder.
	 * @param source data node
	 * @return <code>true</code> if data node is an array else <code>false</code>
	 */
	private boolean isCollection(JsonNode source) {
		JsonNode data = source.get(DATA);
		return data != null && data.isArray();
	}


	/**
	 * Converts input object to byte array.
	 * @param object input object
	 * @return raw bytes
	 * @throws JsonProcessingException
	 * @throws IllegalAccessException
	 */
	public byte [] writeObject(Object object) throws JsonProcessingException, IllegalAccessException {
		ObjectNode result = objectMapper.createObjectNode();

		// Perform initial conversion
		ObjectNode attributesNode = objectMapper.valueToTree(object);

		// Remove id and relationship fields
		Field idField = ID_MAP.get(object.getClass());
		attributesNode.remove(idField.getName());

		// Handle resource identifier
		ObjectNode dataNode = objectMapper.createObjectNode();
		dataNode.put(TYPE, TYPE_ANNOTATIONS.get(object.getClass()).name());

		String resourceId = (String) idField.get(object);
		if (resourceId != null) {
			dataNode.put(ID, resourceId);
		}
		dataNode.set(ATTRIBUTES, attributesNode);

		result.set(DATA, dataNode);

		// Handle relationships (remove from base type and add as relationships)
		List<Field> relationshipFields = RELATIONSHIPS_MAP.get(object.getClass());

		if (relationshipFields != null) {
			ObjectNode relationshipsNode = objectMapper.createObjectNode();
			dataNode.set(RELATIONSHIPS, relationshipsNode);

			for (Field relationshipField : relationshipFields) {
				Object relationshipObject = relationshipField.get(object);

				if (relationshipObject != null) {
					String relationshipName = relationshipField.getAnnotation(Relationship.class).name();

					attributesNode.remove(relationshipField.getName());

					if (relationshipObject instanceof List) {
						ArrayNode dataArrayNode = objectMapper.createArrayNode();

						for (Object element : (List<?>) relationshipObject) {
							String relationshipType = TYPE_ANNOTATIONS.get(element.getClass()).name();
							String idValue = (String) ID_MAP.get(element.getClass()).get(element);

							ObjectNode identifierNode = objectMapper.createObjectNode();
							identifierNode.put(TYPE, relationshipType);
							identifierNode.put(ID, idValue);
							dataArrayNode.add(identifierNode);
						}

						ObjectNode relationshipDataNode = objectMapper.createObjectNode();
						relationshipDataNode.set(DATA, dataArrayNode);
						relationshipsNode.set(relationshipName, relationshipDataNode);

					} else {
						String relationshipType = TYPE_ANNOTATIONS.get(relationshipObject.getClass()).name();
						String idValue = (String) ID_MAP.get(relationshipObject.getClass()).get(relationshipObject);

						ObjectNode identifierNode = objectMapper.createObjectNode();
						identifierNode.put(TYPE, relationshipType);
						identifierNode.put(ID, idValue);


						ObjectNode relationshipDataNode = objectMapper.createObjectNode();
						relationshipDataNode.set(DATA, identifierNode);

						relationshipsNode.set(relationshipName, relationshipDataNode);
					}
				}

			}
		}

		return objectMapper.writeValueAsBytes(result);
	}
}
