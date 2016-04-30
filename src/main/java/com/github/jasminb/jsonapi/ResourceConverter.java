package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Meta;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.github.jasminb.jsonapi.JSONAPISpecConstants.*;

/**
 * JSON API data converter. <br />
 *
 * Provides methods for conversion between JSON API resources to java POJOs and vice versa.
 *
 * @author jbegic
 */
public class ResourceConverter {
	private static final Map<String, Class<?>> TYPE_TO_CLASS_MAPPING = new HashMap<>();
	private static final Map<Class<?>, Type> TYPE_ANNOTATIONS = new HashMap<>();
	private static final Map<Class<?>, Field> ID_MAP = new HashMap<>();
	private static final Map<Class<?>, List<Field>> RELATIONSHIPS_MAP = new HashMap<>();
	private static final Map<Class<?>, Map<String, Class<?>>> RELATIONSHIP_TYPE_MAP = new HashMap<>();
	private static final Map<Class<?>, Map<String, Field>> RELATIONSHIP_FIELD_MAP = new HashMap<>();
	private static final Map<Field, Relationship> FIELD_RELATIONSHIP_MAP = new HashMap<>();
	private static final Map<Class<?>, Class<?>> META_TYPE_MAP = new HashMap<>();
	private static final Map<Class<?>, Field> META_FIELD = new HashMap<>();


	private ObjectMapper objectMapper;

	private RelationshipResolver globalResolver;
	private Map<Class<?>, RelationshipResolver> typedResolvers = new HashMap<>();

	public ResourceConverter(Class<?>... classes) {
		this(null, classes);
	}

	public ResourceConverter(ObjectMapper mapper, Class<?>... classes) {
		for (Class<?> clazz : classes) {
			if (clazz.isAnnotationPresent(Type.class)) {
				Type annotation = clazz.getAnnotation(Type.class);
				TYPE_TO_CLASS_MAPPING.put(annotation.value(), clazz);
				TYPE_ANNOTATIONS.put(clazz, annotation);
				RELATIONSHIP_TYPE_MAP.put(clazz, new HashMap<String, Class<?>>());
				RELATIONSHIP_FIELD_MAP.put(clazz, new HashMap<String, Field>());

				// collecting Relationship fields
				List<Field> relationshipFields = ReflectionUtils.getAnnotatedFields(clazz, Relationship.class, true);

				for (Field relationshipField : relationshipFields) {
					relationshipField.setAccessible(true);

					Relationship relationship = relationshipField.getAnnotation(Relationship.class);
					Class<?> targetType = ReflectionUtils.getFieldType(relationshipField);
					RELATIONSHIP_TYPE_MAP.get(clazz).put(relationship.value(), targetType);
					RELATIONSHIP_FIELD_MAP.get(clazz).put(relationship.value(), relationshipField);
					FIELD_RELATIONSHIP_MAP.put(relationshipField, relationship);
					if (relationship.resolve() && relationship.relType() == null) {
						throw new IllegalArgumentException("@Relationship on " + clazz.getName() + "#" +
								relationshipField.getName() + " with 'resolve = true' must have a relType attribute " +
								"set." );
					}
				}

				RELATIONSHIPS_MAP.put(clazz, relationshipFields);

				// collecting Id fields
				List<Field> idAnnotatedFields = ReflectionUtils.getAnnotatedFields(clazz, Id.class, true);

				if (!idAnnotatedFields.isEmpty() && idAnnotatedFields.size() == 1) {
					Field idField = idAnnotatedFields.get(0);
					idField.setAccessible(true);
					ID_MAP.put(clazz, idField);
				} else {
					if (idAnnotatedFields.isEmpty()) {
						throw new IllegalArgumentException("All resource classes must have a field annotated with the " +
								"@Id annotation");
					} else {
						throw new IllegalArgumentException("Only single @Id annotation is allowed per defined type!");
					}

				}

				// collecting Meta fields
				List<Field> metaFields = ReflectionUtils.getAnnotatedFields(clazz, Meta.class, true);
				if (metaFields.size() > 1) {
					throw new IllegalArgumentException(String.format("Only one meta field is allowed for type '%s'",
							clazz.getCanonicalName()));
				}
				if (metaFields.size() == 1) {
					Field metaField = metaFields.get(0);
					metaField.setAccessible(true);
					Class<?> metaType = ReflectionUtils.getFieldType(metaField);
					META_TYPE_MAP.put(clazz, metaType);
					META_FIELD.put(clazz, metaField);
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
	 * Registers global relationship resolver. This resolver will be used in case relationship is present in the
	 * API response but not provided in the <code>included</code> section and relationship resolving is enabled
	 * trough relationship annotation. <br/>
	 * In case type resolver is registered it will be used instead.
	 * @param resolver resolver instance
	 */
	public void setGlobalResolver(RelationshipResolver resolver) {
		this.globalResolver = resolver;
	}

	/**
	 * Registers relationship resolver for given type. Resolver will be used if relationship resolution is enabled
	 * trough relationship annotation.
	 * @param resolver resolver instance
	 * @param type type
	 */
	public void setTypeResolver(RelationshipResolver resolver, Class<?> type) {
		if (resolver != null) {
			String typeName = ReflectionUtils.getTypeName(type);

			if (typeName != null) {
				typedResolvers.put(type, resolver);
			}
		}
	}

	/**
	 * Converts raw data input into requested target type.
	 * @param data raw-data
	 * @param clazz target object
	 * @param <T>
	 * @return converted object
	 * @throws RuntimeException in case conversion fails
	 */
	public <T> T readObject(byte [] data, Class<T> clazz) {
		return readObjectInternal(data, clazz, null);
	}

	/**
	 * Converts raw data input into requested target type.
	 * @param data raw-data
	 * @param clazz target object
	 * @param <T>
	 * @param resolverState used when resolving recursive relationships;  may be {@code null}
	 * @return converted object
	 * @throws RuntimeException in case conversion fails
	 */
	private <T> T readObjectInternal(byte [] data, Class<T> clazz, ResolverState resolverState) {
		try {
			JsonNode rootNode = objectMapper.readTree(data);

			// Validate
			ValidationUtils.ensureNotError(rootNode);
			ValidationUtils.ensureObject(rootNode);

			Map<String, Object> included = parseIncluded(rootNode, resolverState);

			JsonNode dataNode = rootNode.get(DATA);

			T result = readObjectInternal(dataNode, clazz, included, resolverState);

			// handling of meta node
			if (rootNode.has(META)) {
				Field field = META_FIELD.get(clazz);
				if (field != null) {
					Class<?> metaType = META_TYPE_MAP.get(clazz);
					Object metaObject = objectMapper.treeToValue(rootNode.get(META), metaType);
					field.set(result, metaObject);
				}
			}

			return result;
		} catch (RuntimeException e) {
			throw e;
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
		return readObjectCollectionInternal(data, clazz, null);
	}

	/**
	 * Converts raw-data input into a collection of requested output objects.
	 * @param data raw-data input
	 * @param clazz target type
	 * @param <T>
	 * @param resolverState used when resolving recursive relationships;  may be {@code null}
	 * @return collection of converted elements
	 * @throws RuntimeException in case conversion fails
	 */
	private <T> List<T> readObjectCollectionInternal(byte [] data, Class<T> clazz, ResolverState resolverState) {

		try {
			JsonNode rootNode = objectMapper.readTree(data);

			// Validate
			ValidationUtils.ensureNotError(rootNode);
			ValidationUtils.ensureCollection(rootNode);

			Map<String, Object> included = parseIncluded(rootNode, resolverState);

			List<T> result = new ArrayList<>();

			for (JsonNode element : rootNode.get(DATA)) {
				T pojo = readObjectInternal(element, clazz, included, resolverState);
				result.add(pojo);
			}

			return result;
		} catch (RuntimeException e) {
			throw e;
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
	 * @param resolverState used when resolving recursive relationships;  may be {@code null}
	 * @return converted target object
	 * @throws IOException
	 * @throws IllegalAccessException
	 */
	private <T> T readObjectInternal(JsonNode source, Class<T> clazz, Map<String, Object> cache, ResolverState resolverState)
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
			handleRelationships(source, result, cache, resolverState);

			// Add parsed object to cache
			cache.put(createIdentifier(source), result);
		}


		return result;
	}


	/**
	 * Converts included data and returns it as pairs of its unique identifiers and converted types.
	 * @param parent data source
	 * @param resolverState used when resolving recursive relationships;  may be {@code null}
	 * @return identifier/object pairs
	 * @throws IOException
	 * @throws IllegalAccessException
	 */
	private Map<String, Object> parseIncluded(JsonNode parent, ResolverState resolverState)
			throws IOException, IllegalAccessException, InstantiationException {
		Map<String, Object> result = new HashMap<>();

		if (parent.has(INCLUDED)) {
			// Get resources
			List<Resource> includedResources = getIncludedResources(parent);

			if (!includedResources.isEmpty()) {
				// Add to result
				for (Resource includedResource : includedResources) {
					result.put(includedResource.getIdentifier(), includedResource.getObject());
				}

				ArrayNode includedArray = (ArrayNode) parent.get(INCLUDED);

				for (int i = 0; i < includedResources.size(); i++) {
					Resource resource = includedResources.get(i);

					// Handle relationships
					JsonNode node = includedArray.get(i);
					handleRelationships(node, resource.getObject(), result, resolverState);
				}
			}
		}

		return result;
	}

	/**
	 * Parses out included resources excluding relationships.
	 * @param parent root node
	 * @return map of identifier/resource pairs
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private List<Resource> getIncludedResources(JsonNode parent)
			throws IOException, IllegalAccessException, InstantiationException {
		List<Resource> result = new ArrayList<>();

		if (parent.has(INCLUDED)) {
			for (JsonNode jsonNode : parent.get(INCLUDED)) {
				String type = jsonNode.get(TYPE).asText();

				if (type != null) {
					Class<?> clazz = TYPE_TO_CLASS_MAPPING.get(type);

					if (clazz != null) {
						Object object = readObjectInternal(jsonNode, clazz, null, null);
						result.add(new Resource(createIdentifier(jsonNode), object));
					}
				}
			}
		}

		return result;
	}

	private void handleRelationships(JsonNode source, Object object, Map<String, Object> includedData, ResolverState resolverState)
			throws IllegalAccessException, IOException, InstantiationException {
		JsonNode relationships = source.get(RELATIONSHIPS);

		if (relationships != null) {
			Iterator<String> fields = relationships.fieldNames();

			while (fields.hasNext()) {
				String field = fields.next();

				JsonNode relationship = relationships.get(field);
				Field relationshipField = RELATIONSHIP_FIELD_MAP.get(object.getClass()).get(field);

				if (relationshipField != null) {
					// Get target type
					Class<?> type = RELATIONSHIP_TYPE_MAP.get(object.getClass()).get(field);

					// In case type is not defined, relationship object cannot be processed
					if (type == null) {
						continue;
					}

					// Get resolve flag
					boolean resolveRelationship = FIELD_RELATIONSHIP_MAP.get(relationshipField).resolve();
					RelationshipResolver resolver = getResolver(type);

					// Use resolver if possible
					if (resolveRelationship && resolver != null && relationship.has(LINKS)) {
						String relType = FIELD_RELATIONSHIP_MAP.get(relationshipField).relType().getRelName();

						if (resolverState == null) {
							resolverState = new ResolverState(relationshipField, relType);
						}

						JsonNode linkNode = relationship.get(LINKS).get(relType);

						String link = null;

						if (linkNode != null) {
							link = getLink(linkNode);

							if (resolverState.visited(link)) {
								return;
							}

							if (isCollection(relationship)) {
								relationshipField.set(object, readObjectCollectionInternal(resolver.resolve(link), type, resolverState));
							} else {
								relationshipField.set(object, readObjectInternal(resolver.resolve(link), type, resolverState));
							}
						}
					} else {
						if (isCollection(relationship)) {
							@SuppressWarnings("rawtypes")
							List elements = new ArrayList<>();

							for (JsonNode element : relationship.get(DATA)) {
								Object relationshipObject = parseRelationship(element, type, includedData, resolverState);
								if (relationshipObject != null) {
									elements.add(relationshipObject);
								}
							}
							relationshipField.set(object, elements);
						} else {
							Object relationshipObject = parseRelationship(relationship.get(DATA), type, includedData, resolverState);
							if (relationshipObject != null) {
								relationshipField.set(object, relationshipObject);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Accepts a JsonNode which encapsulates a link.  The link may be represented as a simple string or as
	 * <a href="http://jsonapi.org/format/#document-links">link</a> object.  This method introspects on the
	 * {@code linkNode}, returning the value of the {@code href} member, if it exists, or returns the string form
	 * of the {@code linkNode} if it doesn't.
	 *
	 * @param linkNode a JsonNode representing a link
	 * @return the link URL
	 */
	private String getLink(JsonNode linkNode) {
		// Handle both representations of a link: as a string or as an object
		// http://jsonapi.org/format/#document-links (v1.0)
		if (linkNode.has(HREF)) {
			// object form
			return linkNode.get(HREF).asText();
		}
		return linkNode.asText();
	}

	/**
	 * Creates relationship object by consuming provided 'data' node.
	 * @param relationshipDataNode relationship data node
	 * @param type object type
	 * @param cache object cache
	 * @param resolverState used when resolving recursive relationships;  may be {@code null}
	 * @return created object or <code>null</code> in case data node is not valid
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private Object parseRelationship(JsonNode relationshipDataNode, Class<?> type, Map<String, Object> cache, ResolverState resolverState)
			throws IOException, IllegalAccessException, InstantiationException {
		if (ValidationUtils.isRelationshipParsable(relationshipDataNode)) {
			String identifier = createIdentifier(relationshipDataNode);

			if (cache.containsKey(identifier)) {
				return cache.get(identifier);
			} else {
				return readObjectInternal(relationshipDataNode, type, cache, resolverState);
			}
		}

		return null;
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
		ObjectNode dataNode = getDataNode(object);
		ObjectNode result = objectMapper.createObjectNode();

		result.set(DATA, dataNode);

		return objectMapper.writeValueAsBytes(result);
	}

	private ObjectNode getDataNode(Object object) throws IllegalAccessException {

		// Perform initial conversion
		ObjectNode attributesNode = objectMapper.valueToTree(object);

		// Remove id, meta and relationship fields
		Field idField = ID_MAP.get(object.getClass());
		attributesNode.remove(idField.getName());

		Field metaField = META_FIELD.get(object.getClass());
		if (metaField != null) {
			attributesNode.remove(metaField.getName());
		}

		// Handle resource identifier
		ObjectNode dataNode = objectMapper.createObjectNode();
		dataNode.put(TYPE, TYPE_ANNOTATIONS.get(object.getClass()).value());

		String resourceId = (String) idField.get(object);
		if (resourceId != null) {
			dataNode.put(ID, resourceId);
		}
		dataNode.set(ATTRIBUTES, attributesNode);


		// Handle relationships (remove from base type and add as relationships)
		List<Field> relationshipFields = RELATIONSHIPS_MAP.get(object.getClass());

		if (relationshipFields != null) {
			ObjectNode relationshipsNode = objectMapper.createObjectNode();

			for (Field relationshipField : relationshipFields) {
				Object relationshipObject = relationshipField.get(object);

				if (relationshipObject != null) {
					attributesNode.remove(relationshipField.getName());

					Relationship relationship = FIELD_RELATIONSHIP_MAP.get(relationshipField);

					// In case serialisation is disabled for a given relationship, skipp it
					if (!relationship.serialise()) {
						continue;
					}

					String relationshipName = relationship.value();

					if (relationshipObject instanceof List) {
						ArrayNode dataArrayNode = objectMapper.createArrayNode();

						for (Object element : (List<?>) relationshipObject) {
							String relationshipType = TYPE_ANNOTATIONS.get(element.getClass()).value();
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
						String relationshipType = TYPE_ANNOTATIONS.get(relationshipObject.getClass()).value();
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

			if (relationshipsNode.size() > 0) {
				dataNode.set(RELATIONSHIPS, relationshipsNode);
			}
		}
		return dataNode;
	}

	/**
	 * Converts input object to byte array.
	 *
	 * @param objects List of input objects
	 * @return raw bytes
	 * @throws JsonProcessingException
	 * @throws IllegalAccessException
	 */
	public <T> byte[] writeObjectCollection(Iterable<T> objects) throws JsonProcessingException, IllegalAccessException {
		ArrayNode results = objectMapper.createArrayNode();

		for(T object : objects) {
			results.add(getDataNode(object));
		}

		ObjectNode result = objectMapper.createObjectNode();
		result.set(DATA, results);
		return objectMapper.writeValueAsBytes(result);
	}


	/**
	 * Checks if provided type is registered with this converter instance.
	 * @param type class to check
	 * @return returns <code>true</code> if type is registered, else <code>false</code>
	 */
	public boolean isRegisteredType(Class<?> type) {
		return TYPE_ANNOTATIONS.containsKey(type);
	}

	/**
	 * Returns relationship resolver for given type. In case no specific type resolver is registered, global resolver
	 * is returned.
	 * @param type relationship object type
	 * @return relationship resolver or <code>null</code>
	 */
	private RelationshipResolver getResolver(Class<?> type) {
		RelationshipResolver resolver = typedResolvers.get(type);
		return resolver != null ? resolver : globalResolver;
	}

	private static class Resource {
		private String identifier;
		private Object object;

		public Resource(String identifier, Object resource) {
			this.identifier = identifier;
			this.object = resource;
		}

		public String getIdentifier() {
			return identifier;
		}

		public Object getObject() {
			return object;
		}
	}


}
