package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.jasminb.jsonapi.annotations.Relationship;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.jasminb.jsonapi.JSONAPISpecConstants.*;

/**
 * JSON API data converter. <br />
 *
 * Provides methods for conversion between JSON API resources to java POJOs and vice versa.
 *
 * @author jbegic
 */
public class ResourceConverter {
	private final ConverterConfiguration configuration;
	private final ObjectMapper objectMapper;
	private final Map<Class<?>, RelationshipResolver> typedResolvers = new HashMap<>();
	private final ResourceCache resourceCache;
	private final Set<DeserializationFeature> deserializationFeatures = DeserializationFeature.getDefaultFeatures();

	private RelationshipResolver globalResolver;

	/**
	 * Creates new ResourceConverter.
	 * <p>
	 *     All classes that should be handled by instance of {@link ResourceConverter} must be registered
	 *     when creating a new instance of it.
	 * </p>
	 * @param classes {@link Class} array of classes to be handled by this resource converter instance
	 */
	public ResourceConverter(Class<?>... classes) {
		this(null, classes);
	}

	/**
	 * Creates new ResourceConverter.
	 * @param mapper {@link ObjectMapper} custom mapper to be used for resource parsing
	 * @param classes {@link Class} array of classes to be handled by this resource converter instance
	 */
	public ResourceConverter(ObjectMapper mapper, Class<?>... classes) {
		this.configuration = new ConverterConfiguration(classes);

		// Set custom mapper if provided
		if (mapper != null) {
			objectMapper = mapper;
		} else {
			objectMapper = new ObjectMapper();
		}

		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

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

	/**
	 * Reads JSON API spec document and converts it into target type.
	 * @param data {@link byte} raw data (server response)
	 * @param clazz {@link Class} target type
	 * @param <T> type
	 * @return {@link JSONAPIDocument}
	 */
	public <T> JSONAPIDocument<T> readDocument(InputStream data, Class<T> clazz) {
		try {
			resourceCache.init();

			JsonNode rootNode = objectMapper.readTree(data);

			// Validate
			ValidationUtils.ensureNotError(objectMapper, rootNode);
			ValidationUtils.ensureObject(rootNode);

			resourceCache.cache(parseIncluded(rootNode));

			JsonNode dataNode = rootNode.get(DATA);


			T resourceObject = readObject(dataNode, clazz, true);

			JSONAPIDocument<T> result = new JSONAPIDocument<>(resourceObject);


			// Handle top-level meta
			if (rootNode.has(META)) {
				result.setMeta(mapMeta(rootNode.get(META)));
			}

			// Handle top-level links
			if (rootNode.has(LINKS)) {
				result.setLinks(new Links(mapLinks(rootNode.get(LINKS))));
			}

			return result;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			resourceCache.clear();
		}
	}

	/**
	 * Reads JSON API spec document and converts it into collection of target type objects.
	 * @param data {@link byte} raw data (server response)
	 * @param clazz {@link Class} target type
	 * @param <T> type
	 * @return {@link JSONAPIDocument}
	 */
	public <T> JSONAPIDocument<List<T>> readDocumentCollection(InputStream data, Class<T> clazz) {
		try {
			resourceCache.init();

			JsonNode rootNode = objectMapper.readTree(data);

			// Validate
			ValidationUtils.ensureNotError(objectMapper, rootNode);
			ValidationUtils.ensureCollection(rootNode);

			resourceCache.cache(parseIncluded(rootNode));

			List<T> resourceList = new ArrayList<>();

			for (JsonNode element : rootNode.get(DATA)) {
				T pojo = readObject(element, clazz, true);
				resourceList.add(pojo);
			}

			JSONAPIDocument<List<T>> result = new JSONAPIDocument<>(resourceList);

			// Handle top-level meta
			if (rootNode.has(META)) {
				result.setMeta(mapMeta(rootNode.get(META)));
			}

			// Handle top-level links
			if (rootNode.has(LINKS)) {
				result.setLinks(new Links(mapLinks(rootNode.get(LINKS))));
			}

			return result;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			resourceCache.clear();
		}
	}

	/**
	 * Converts provided input into a target object. After conversion completes any relationships defined are resolved.
	 * @param source JSON source
	 * @param clazz target type
	 * @param <T> type
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
				if (clazz.isInterface()) {
					result = null;
				} else {
					result = clazz.newInstance();
				}
			}

			// Handle meta
			if (source.has(META)) {
				Field field = configuration.getMetaField(clazz);
				if (field != null) {
					Class<?> metaType = configuration.getMetaType(clazz);
					Object metaObject = objectMapper.treeToValue(source.get(META), metaType);
					field.set(result, metaObject);
				}
			}

			// Handle links
			if (source.has(LINKS)) {
				Field linkField = configuration.getLinksField(clazz);
				if (linkField != null) {
					linkField.set(result, new Links(mapLinks(source.get(LINKS))));
				}
			}

			if(result != null) {
				// Add parsed object to cache
				resourceCache.cache(identifier, result);

				// Set object id
				setIdValue(result, source.get(ID));

				if (handleRelationships) {
					// Handle relationships
					handleRelationships(source, result);
				}
			}
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
					Class<?> clazz = configuration.getTypeClass(type);

					if (clazz != null) {
						Object object = readObject(jsonNode, clazz, false);
						if (object != null) {
							result.add(new Resource(createIdentifier(jsonNode), object));
						}
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
				Field relationshipField = configuration.getRelationshipField(object.getClass(), field);

				if (relationshipField != null) {
					// Get target type
					Class<?> type = configuration.getRelationshipType(object.getClass(), field);

					// In case type is not defined, relationship object cannot be processed
					if (type == null) {
						continue;
					}

					// Get resolve flag
					boolean resolveRelationship = configuration.getFieldRelationship(relationshipField).resolve();
					RelationshipResolver resolver = getResolver(type);

					// Use resolver if possible
					if (resolveRelationship && resolver != null && relationship.has(LINKS)) {
						String relType = configuration.getFieldRelationship(relationshipField).relType().getRelName();
						JsonNode linkNode = relationship.get(LINKS).get(relType);

						String link;

						if (linkNode != null && ((link = getLink(linkNode)) != null)) {
							if (isCollection(relationship)) {
								relationshipField.set(object,
										readDocumentCollection(new ByteArrayInputStream(resolver.resolve(link)), type).get());
							} else {
								relationshipField.set(object, readDocument(new ByteArrayInputStream(resolver.resolve(link)), type).get());
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
		JsonNode idNode = object.get(ID);

		String id = idNode != null ? idNode.asText().trim() : "";

		if (id.isEmpty() && deserializationFeatures.contains(DeserializationFeature.REQUIRE_RESOURCE_ID)) {
			throw new IllegalArgumentException("Resource must have an non null and non-empty 'id' attribute!");
		}

		String type = object.get(TYPE).asText();
		return type.concat(id);
	}

	/**
	 * Sets an id attribute value to a target object.
	 * @param target target POJO
	 * @param idValue id node
	 * @throws IllegalAccessException thrown in case target field is not accessible
	 */
	private void setIdValue(Object target, JsonNode idValue) throws IllegalAccessException {
		Field idField = configuration.getIdField(target.getClass());

		// By specification, id value is always a String type
		if (idValue != null) {
			idField.set(target, idValue.asText());
		}
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
		Field idField = configuration.getIdField(object.getClass());
		attributesNode.remove(idField.getName());

		Field metaField = configuration.getMetaField(object.getClass());
		if (metaField != null) {
			attributesNode.remove(metaField.getName());
		}

		// Handle resource identifier
		ObjectNode dataNode = objectMapper.createObjectNode();
		dataNode.put(TYPE, configuration.getTypeName(object.getClass()));

		String resourceId = (String) idField.get(object);
		if (resourceId != null) {
			dataNode.put(ID, resourceId);
		}
		dataNode.set(ATTRIBUTES, attributesNode);


		// Handle relationships (remove from base type and add as relationships)
		List<Field> relationshipFields = configuration.getRelationshipFields(object.getClass());

		if (relationshipFields != null) {
			ObjectNode relationshipsNode = objectMapper.createObjectNode();

			for (Field relationshipField : relationshipFields) {
				Object relationshipObject = relationshipField.get(object);

				if (relationshipObject != null) {
					attributesNode.remove(relationshipField.getName());

					Relationship relationship = configuration.getFieldRelationship(relationshipField);

					// In case serialisation is disabled for a given relationship, skipp it
					if (!relationship.serialise()) {
						continue;
					}

					String relationshipName = relationship.value();

					if (relationshipObject instanceof List) {
						ArrayNode dataArrayNode = objectMapper.createArrayNode();

						for (Object element : (List<?>) relationshipObject) {
							String relationshipType = configuration.getTypeName(element.getClass());
							String idValue = (String) configuration.getIdField(element.getClass()).get(element);

							ObjectNode identifierNode = objectMapper.createObjectNode();
							identifierNode.put(TYPE, relationshipType);
							identifierNode.put(ID, idValue);
							dataArrayNode.add(identifierNode);
						}

						ObjectNode relationshipDataNode = objectMapper.createObjectNode();
						relationshipDataNode.set(DATA, dataArrayNode);
						relationshipsNode.set(relationshipName, relationshipDataNode);

					} else {
						String relationshipType = configuration.getTypeName(relationshipObject.getClass());
						String idValue = (String) configuration.getIdField(relationshipObject.getClass())
								.get(relationshipObject);

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
		return configuration.isRegisteredType(type);
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

	/**
	 * Deserializes a <a href="http://jsonapi.org/format/#document-links">JSON-API links object</a> to a {@code Map}
	 * keyed by the link name.
	 * <p>
	 * The {@code linksObject} may represent links in string form or object form; both are supported by this method.
	 * </p>
	 * <p>
	 * E.g.
	 * <pre>
	 * "links": {
	 *   "self": "http://example.com/posts"
	 * }
	 * </pre>
	 * </p>
	 * <p>
	 * or
	 * <pre>
	 * "links": {
	 *   "related": {
	 *     "href": "http://example.com/articles/1/comments",
	 *     "meta": {
	 *       "count": 10
	 *     }
	 *   }
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param linksObject a {@code JsonNode} representing a links object
	 * @return a {@code Map} keyed by link name
	 */
	private Map<String, Link> mapLinks(JsonNode linksObject) {
		Map<String, Link> result = new HashMap<>();

		Iterator<Map.Entry<String, JsonNode>> linkItr = linksObject.fields();

		while (linkItr.hasNext()) {
			Map.Entry<String, JsonNode> linkNode = linkItr.next();
			Link linkObj = new Link();

			linkObj.setHref(
					getLink(
							linkNode.getValue()));

			if (linkNode.getValue().has(META)) {
				linkObj.setMeta(
						mapMeta(
								linkNode.getValue().get(META)));
			}

			result.put(linkNode.getKey(), linkObj);
		}

		return result;
	}

	/**
	 * Deserializes a <a href="http://jsonapi.org/format/#document-meta">JSON-API meta object</a> to a {@code Map}
	 * keyed by the member names.  Because {@code meta} objects contain arbitrary information, the values in the
	 * map are of unknown type.
	 *
	 * @param metaNode a JsonNode representing a meta object
	 * @return a Map of the meta information, keyed by member name.
	 */
	private Map<String, ?> mapMeta(JsonNode metaNode) {
		JsonParser p = objectMapper.treeAsTokens(metaNode);
		MapType mapType = TypeFactory.defaultInstance()
				.constructMapType(HashMap.class, String.class, Object.class);
		try {
			return objectMapper.readValue(p, mapType);
		} catch (IOException e) {
			// TODO: log? No recovery.
		}

		return null;
	}

	/**
	 * Adds (enables) new deserialization option.
	 * @param option {@link DeserializationFeature} option
	 */
	public void enableDeserializationOption(DeserializationFeature option) {
		this.deserializationFeatures.add(option);
	}

	/**
	 * Removes (disables) existing deserialization option.
	 * @param option {@link DeserializationFeature} feature to disable
	 */
	public void disableDeserializationOption(DeserializationFeature option) {
		this.deserializationFeatures.remove(option);
	}
}
