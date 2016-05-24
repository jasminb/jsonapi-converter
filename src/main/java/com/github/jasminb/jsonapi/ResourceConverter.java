package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Links;
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
	private static final Map<Class<?>, Field> LINKS_FIELD = new HashMap<>();

	private ObjectMapper objectMapper;
	private JSONAPIParser jsonapiParser;

	private RelationshipResolver globalResolver;
	private Map<Class<?>, RelationshipResolver> typedResolvers = new HashMap<>();

	private ResourceCache resourceCache;

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

				// collecting Links fields
				List<Field> linksFields = ReflectionUtils.getAnnotatedFields(clazz, Links.class, true);
				if (linksFields.size() > 1) {
					throw new IllegalArgumentException(String.format("Only one @Links field is allowed for type '%s'",
							clazz.getCanonicalName()));
				}
				if (linksFields.size() == 1) {
					Field linksField = linksFields.get(0);
					linksField.setAccessible(true);
					Class<?> linksType = ReflectionUtils.getFieldType(linksField);
					if (linksType.isAssignableFrom(LinksData.class)) {
						LINKS_FIELD.put(clazz, linksField);
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
		jsonapiParser = new JSONAPIParser(objectMapper);

		resourceCache = new ResourceCache();
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

	interface JsonApiDataParser<T> {

		T parse(JsonNode rootNode) throws IllegalAccessException, IOException, InstantiationException;
	}

	private void setMetaFrom(Object result, Class<?> clazz, Field field, JsonNode... containerNodes) throws
			IllegalAccessException,
			JsonProcessingException {
		for (JsonNode containerNode : containerNodes) {
			if (containerNode.has(META)) {
				Class<?> metaType = META_TYPE_MAP.get(clazz);
				Object metaObject = objectMapper.treeToValue(containerNode.get(META), metaType);
				field.set(result, metaObject);
			}
		}
	}

	private class JsonApiResourceParser<T> implements JsonApiDataParser<T> {

		private final Class<T> clazz;

		private JsonApiResourceParser(Class<T> clazz) {
			this.clazz = clazz;
		}

		@Override
		public T parse(JsonNode rootNode) throws IllegalAccessException, IOException, InstantiationException {

			ValidationUtils.ensureObject(rootNode);
			JsonNode dataNode = rootNode.get(DATA);

			T result = readObject(dataNode, clazz, true);

			// strangeness here - unit tests expect document-level 'meta' field is placed in Resource object annotation
			// but we could have a resource-level metadata, too, which may be the right choice for meta to place there
			// especially because document-level 'meta' field will be present in the JsonApiDocument.
			// resource-level will have been set in the readObject() method above, but to make unit tests pass for now,
			// we'll set the document-level (rootNode) meta on the object, maybe overwriting the resource-level
			// for now:
			Field metaField = META_FIELD.get(clazz);
			if (metaField != null) {
				setMetaFrom(result, clazz, metaField, rootNode);
			}

			// handling of links node
			if (dataNode.has(LINKS)) {
				Field field = LINKS_FIELD.get(clazz);
				if (field != null) {
					field.set(result, jsonapiParser.toLinksData(dataNode.get(LINKS)));
				}
			}
			return result;
		}
	}

	private class JsonApiCollectionParser<T> implements JsonApiDataParser<List<T>> {

		private final Class<T> clazz;

		private JsonApiCollectionParser(Class<T> clazz) {
			this.clazz = clazz;
		}

		@Override
		public List<T> parse(JsonNode rootNode) throws IllegalAccessException, IOException, InstantiationException {

			ValidationUtils.ensureCollection(rootNode);
			JsonNode dataNode = rootNode.get(DATA);

			List<T> result = new ArrayList<>();

			for (JsonNode element : dataNode) {
				T pojo = readObject(element, clazz, true);
				result.add(pojo);
			}

			return result;
		}
	}


	<T> JsonApiDocument<T> readDocument(byte[] bytes, JsonApiDataParser<T> dataParser) {
		try {
			resourceCache.init();

			JsonNode rootNode = objectMapper.readTree(bytes);

			// Validate
			ValidationUtils.ensureNotError(rootNode);

			resourceCache.cache(parseIncluded(rootNode));

			T data = dataParser.parse(rootNode);

			LinksData linksData = null;
			// handling of links node
			if (rootNode.has(LINKS)) {
				linksData = jsonapiParser.toLinksData(rootNode.get(LINKS));
			}

			return new JsonApiDocument<>(data, linksData);

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			resourceCache.clear();
		}
	}

	/**
	 * Converts raw-data input into a collection of requested output objects.
	 *
	 * @param bytes raw-data input
	 * @param clazz target type
	 * @return collection of converted elements
	 * @throws RuntimeException in case conversion fails
	 */
	public <T> JsonApiDocument<List<T>> readCollectionDocument(byte[] bytes, Class<T> clazz) {

		return readDocument(bytes, new JsonApiCollectionParser<>(clazz));
	}

	public <T> List<T> readObjectCollection(byte[] bytes, Class<T> clazz) {

		return readCollectionDocument(bytes, clazz).getData();
	}

	public <T> JsonApiDocument<T> readObjectDocument(byte[] bytes, Class<T> clazz) {
		return readDocument(bytes, new JsonApiResourceParser<>(clazz));
	}

	public <T> T readObject(byte[] bytes, Class<T> clazz) {
		return readObjectDocument(bytes, clazz).getData();
	}

	/**
	 * Converts provided input into a target object. After conversion completes any relationships defined are resolved.
	 * @param source JSON source
	 * @param clazz target type
	 * @param <T>
	 * @return converted target object
	 * @throws IOException
	 * @throws IllegalAccessException
	 */
	private <T> T readObject(JsonNode source, Class<T> clazz, boolean handleRelationships)
			throws IOException, IllegalAccessException, InstantiationException {
		String identifier = createIdentifier(source);

		T result = (T) resourceCache.get(identifier);

		if (result == null) {
			if (source.has(ATTRIBUTES)) {
				result = objectMapper.treeToValue(source.get(ATTRIBUTES), clazz);
			} else {
				result = clazz.newInstance();
			}

			// Add parsed object to cache
			resourceCache.cache(identifier, result);

			// Set object id
			setIdValue(result, source.get(ID));

			if (handleRelationships) {
				// Handle relationships
				handleRelationships(source, result);
			}
		}

		Field metaField = META_FIELD.get(clazz);
		if (metaField != null) {
			setMetaFrom(result, clazz, metaField, source);
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
					handleRelationships(node, resource.getObject());
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
						Object object = readObject(jsonNode, clazz, false);
						result.add(new Resource(createIdentifier(jsonNode), object));
					}
				}
			}
		}

		return result;
	}

	private void handleRelationships(JsonNode source, Object object)
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
						JsonNode linkNode = relationship.get(LINKS).get(relType);

						String link;

						if (linkNode != null && ((link = getLink(linkNode)) != null)) {
							if (isCollection(relationship)) {
								relationshipField.set(object, readObjectCollection(resolver.resolve(link), type));
							} else {
								relationshipField.set(object, readObject(resolver.resolve(link), type));
							}
						}
					} else {
						if (isCollection(relationship)) {
							@SuppressWarnings("rawtypes")
							List elements = new ArrayList<>();

							for (JsonNode element : relationship.get(DATA)) {
								Object relationshipObject = parseRelationship(element, type);
								if (relationshipObject != null) {
									elements.add(relationshipObject);
								}
							}
							relationshipField.set(object, elements);
						} else {
							Object relationshipObject = parseRelationship(relationship.get(DATA), type);
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
	 * <p>
	 * <em>Package-private for unit testing.</em>
	 * </p>
	 * @param linkNode a JsonNode representing a link, may return {@code null}
	 * @return the link URL
	 */
	String getLink(JsonNode linkNode) {
		// Handle both representations of a link: as a string or as an object
		// http://jsonapi.org/format/#document-links (v1.0)
		if (linkNode.has(HREF)) {
			// object form
			return linkNode.get(HREF).asText();
		}
		return linkNode.asText(null);
	}

	/**
	 * Creates relationship object by consuming provided 'data' node.
	 * @param relationshipDataNode relationship data node
	 * @param type object type
	 * @return created object or <code>null</code> in case data node is not valid
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private Object parseRelationship(JsonNode relationshipDataNode, Class<?> type)
			throws IOException, IllegalAccessException, InstantiationException {
		if (ValidationUtils.isRelationshipParsable(relationshipDataNode)) {
			String identifier = createIdentifier(relationshipDataNode);

			if (resourceCache.contains(identifier)) {
				return resourceCache.get(identifier);
			} else {
				// Never cache relationship objects
				resourceCache.lock();
				try {
					return readObject(relationshipDataNode, type, true);
				} finally {
					resourceCache.unlock();
				}
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
