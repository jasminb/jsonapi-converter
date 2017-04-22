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
import com.github.jasminb.jsonapi.annotations.Type;
import com.github.jasminb.jsonapi.exceptions.DocumentSerializationException;
import com.github.jasminb.jsonapi.models.errors.Error;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
	private final Set<SerializationFeature> serializationFeatures = SerializationFeature.getDefaultFeatures();

	private RelationshipResolver globalResolver;
	
	private String baseURL;

	/**
	 * Creates new ResourceConverter.
	 * <p>
	 *     All classes that should be handled by instance of {@link ResourceConverter} must be registered
	 *     when creating a new instance of it.
	 * </p>
	 * @param classes {@link Class} array of classes to be handled by this resource converter instance
	 */
	public ResourceConverter(Class<?>... classes) {
		this(null, null, classes);
	}
	
	/**
	 * Creates new ResourceConverter.
	 * <p>
	 *     All classes that should be handled by instance of {@link ResourceConverter} must be registered
	 *     when creating a new instance of it.
	 * </p>
	 * @param baseURL {@link String} base URL, eg. https://api.mysite.com
	 * @param classes {@link Class} array of classes to be handled by this resource converter instance
	 */
	public ResourceConverter(String baseURL, Class<?>... classes) {
		this(null, baseURL, classes);
	}
	
	public ResourceConverter(ObjectMapper mapper, Class<?>... classes) {
		this(mapper, null, classes);
	}

	/**
	 * Creates new ResourceConverter.
	 * @param mapper {@link ObjectMapper} custom mapper to be used for resource parsing
	 * @param baseURL {@link String} base URL, eg. https://api.mysite.com
	 * @param classes {@link Class} array of classes to be handled by this resource converter instance
	 */
	public ResourceConverter(ObjectMapper mapper, String baseURL, Class<?>... classes) {
		this.configuration = new ConverterConfiguration(classes);
		this.baseURL = baseURL != null ? baseURL : "";

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
	* Converts raw data input into requested target type.
	* @param data raw data
	* @param clazz target object
	* @param <T> type
	* @return converted object
	* @throws RuntimeException in case conversion fails
	*/
	@Deprecated
	public <T> T readObject(byte [] data, Class<T> clazz) {
		return readDocument(data, clazz).get();
	}

	/**
	 * Converts rawdata input into a collection of requested output objects.
	 * @param data raw data input
	 * @param clazz target type
	 * @param <T> type
	 * @return collection of converted elements
	 * @throws RuntimeException in case conversion fails
	 */
	@Deprecated
	public <T> List<T> readObjectCollection(byte [] data, Class<T> clazz) {
		return readDocumentCollection(data, clazz).get();
	}

	/**
	 * Reads JSON API spec document and converts it into target type.
	 * @param data {@link byte} raw data (server response)
	 * @param clazz {@link Class} target type
	 * @param <T> type
	 * @return {@link JSONAPIDocument}
	 */
	public <T> JSONAPIDocument<T> readDocument(byte[] data, Class<T> clazz) {
		return readDocument(new ByteArrayInputStream(data), clazz);
	}

	/**
	 * Reads JSON API spec document and converts it into target type.
	 * @param dataStream {@link byte} raw dataStream (server response)
	 * @param clazz {@link Class} target type
	 * @param <T> type
	 * @return {@link JSONAPIDocument}
	 */
	public <T> JSONAPIDocument<T> readDocument(InputStream dataStream, Class<T> clazz) {
		try {
			resourceCache.init();

			JsonNode rootNode = objectMapper.readTree(dataStream);

			// Validate
			ValidationUtils.ensureNotError(objectMapper, rootNode);
			ValidationUtils.ensureValidResource(rootNode);

			resourceCache.cache(parseIncluded(rootNode));

			JsonNode dataNode = rootNode.get(DATA);

			JSONAPIDocument<T> result;

			if (dataNode != null && dataNode.isObject()) {
				T resourceObject = readObject(dataNode, clazz, true);
				result = new JSONAPIDocument<>(resourceObject, objectMapper);
			} else {
				result = new JSONAPIDocument<>(null, objectMapper);
			}

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
	public <T> JSONAPIDocument<List<T>> readDocumentCollection(byte[] data, Class<T> clazz) {
		return readDocumentCollection(new ByteArrayInputStream(data), clazz);
	}
	/**
	 * Reads JSON API spec document and converts it into collection of target type objects.
	 * @param dataStream {@link InputStream} input stream
	 * @param clazz {@link Class} target type
	 * @param <T> type
	 * @return {@link JSONAPIDocument}
	 */
	public <T> JSONAPIDocument<List<T>> readDocumentCollection(InputStream dataStream, Class<T> clazz) {
		try {
			resourceCache.init();

			JsonNode rootNode = objectMapper.readTree(dataStream);

			// Validate
			ValidationUtils.ensureNotError(objectMapper, rootNode);
			ValidationUtils.ensureValidResource(rootNode);

			resourceCache.cache(parseIncluded(rootNode));

			List<T> resourceList = new ArrayList<>();

			if (rootNode.has(DATA) && rootNode.get(DATA).isArray()) {
				for (JsonNode element : rootNode.get(DATA)) {
					T pojo = readObject(element, clazz, true);
					resourceList.add(pojo);
				}
			}
			
			JSONAPIDocument<List<T>> result = new JSONAPIDocument<>(resourceList, objectMapper);

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
			Class<?> type = getActualType(source, clazz);

			if (source.has(ATTRIBUTES)) {
				result = (T) objectMapper.treeToValue(source.get(ATTRIBUTES), type);
			} else {
				if (type.isInterface()) {
					result = null;
				} else {
					result = (T) objectMapper.treeToValue(objectMapper.createObjectNode(), type);
				}
			}

			// Handle meta
			if (source.has(META)) {
				Field field = configuration.getMetaField(type);
				if (field != null) {
					Class<?> metaType = configuration.getMetaType(type);
					Object metaObject = objectMapper.treeToValue(source.get(META), metaType);
					field.set(result, metaObject);
				}
			}

			// Handle links
			if (source.has(LINKS)) {
				Field linkField = configuration.getLinksField(type);
				if (linkField != null) {
					linkField.set(result, new Links(mapLinks(source.get(LINKS))));
				}
			}

			if (result != null) {
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
				
				Class<?> clazz = configuration.getTypeClass(type);
				
				if (clazz != null) {
					Object object = readObject(jsonNode, clazz, false);
					if (object != null) {
						result.add(new Resource(createIdentifier(jsonNode), object));
					}
				} else if (!deserializationFeatures.contains(DeserializationFeature.ALLOW_UNKNOWN_INCLUSIONS)) {
					throw new IllegalArgumentException("Included section contains unknown resource type: " + type);
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
					
					// Handle meta if present
					if (relationship.has(META)) {
						Field relationshipMetaField = configuration.getRelationshipMetaField(object.getClass(), field);
						
						if (relationshipMetaField != null) {
							relationshipMetaField.set(object, objectMapper.treeToValue(relationship.get(META),
									configuration.getRelationshipMetaType(object.getClass(), field)));
						}
					}
					
					// Handle links if present
					if (relationship.has(LINKS)) {
						Field relationshipLinksField = configuration.getRelationshipLinksField(object.getClass(), field);
						if (relationshipLinksField != null) {
							Links links = new Links(mapLinks(relationship.get(LINKS)));
							relationshipLinksField.set(object, links);
						}
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
							Collection elements = createCollectionInstance(relationshipField.getType());

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
		ResourceIdHandler idHandler = configuration.getIdHandler(target.getClass());
		
		if (idValue != null) {
			idField.set(target, idHandler.fromString(idValue.asText()));
		}
	}
	
	/**
	 * Reads @Id value from provided source object.
	 *
	 * @param source object to read @Id value from
	 * @return {@link String} id or <code>null</code>
	 * @throws IllegalAccessException
	 */
	private String getIdValue(Object source) throws IllegalAccessException {
		Field idField = configuration.getIdField(source.getClass());
		ResourceIdHandler handler = configuration.getIdHandler(source.getClass());
		
		return handler.asString(idField.get(source));
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
	@Deprecated
	public byte [] writeObject(Object object) throws JsonProcessingException, IllegalAccessException {
		try {
			return writeDocument(new JSONAPIDocument<>(object));
		} catch (DocumentSerializationException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Serializes provided {@link JSONAPIDocument} into JSON API Spec compatible byte representation.
	 * @param document {@link JSONAPIDocument} document to serialize
	 * @return serialized content in bytes
	 * @throws DocumentSerializationException thrown in case serialization fails
	 */
	public byte [] writeDocument(JSONAPIDocument<?> document) throws DocumentSerializationException {
		return writeDocument(document, null);
	}
	
	/**
	 * Serializes provided {@link JSONAPIDocument} into JSON API Spec compatible byte representation.
	 * @param document {@link JSONAPIDocument} document to serialize
	 * @param settings {@link SerializationSettings} settings that override global serialization settings
	 * @return serialized content in bytes
	 * @throws DocumentSerializationException thrown in case serialization fails
	 */
	public byte [] writeDocument(JSONAPIDocument<?> document, SerializationSettings settings)
			throws DocumentSerializationException {
		try {
			resourceCache.init();

			Map<String, ObjectNode> includedDataMap = new HashMap<>();
			
			ObjectNode result = objectMapper.createObjectNode();
			
			// Serialize data if present
			if (document.get() != null) {
				ObjectNode dataNode = getDataNode(document.get(), includedDataMap, settings);
				result.set(DATA, dataNode);
				result = addIncludedSection(result, includedDataMap);
			}
			
			// Serialize errors if present
			if (document.getErrors() != null) {
				ArrayNode errorsNode = objectMapper.createArrayNode();
				for (Error error : document.getErrors()) {
					errorsNode.add(objectMapper.valueToTree(error));
				}
				
				result.set(ERRORS, errorsNode);
			}
			
			// Serialize global links and meta
			serializeMeta(document, result, settings);
			serializeLinks(document, result, settings);
			return objectMapper.writeValueAsBytes(result);
		} catch (Exception e) {
			throw new DocumentSerializationException(e);
		} finally {
			resourceCache.clear();
		}
	}

	private void serializeMeta(JSONAPIDocument<?> document, ObjectNode resultNode, SerializationSettings settings) {
		// Handle global links and meta
		if (document.getMeta() != null && !document.getMeta().isEmpty() && shouldSerializeMeta(settings)) {
			resultNode.set(META, objectMapper.valueToTree(document.getMeta()));
		}
	}

	private void serializeLinks(JSONAPIDocument<?> document, ObjectNode resultNode, SerializationSettings settings) {
		if (document.getLinks() != null && !document.getLinks().getLinks().isEmpty() &&
				shouldSerializeLinks(settings)) {
			resultNode.set(LINKS, objectMapper.valueToTree(document.getLinks()).get(LINKS));
		}
	}
	
	/**
	 * Serializes provided {@link JSONAPIDocument} into JSON API Spec compatible byte representation.
	 * @param documentCollection {@link JSONAPIDocument} document collection to serialize
	 * @return serialized content in bytes
	 * @throws DocumentSerializationException thrown in case serialization fails
	 */
	public byte [] writeDocumentCollection(JSONAPIDocument<? extends Iterable<?>> documentCollection)
			throws DocumentSerializationException {
		return writeDocumentCollection(documentCollection, null);
	}
	
	/**
	 * Serializes provided {@link JSONAPIDocument} into JSON API Spec compatible byte representation.
	 * @param documentCollection {@link JSONAPIDocument} document collection to serialize
	 * @param serializationSettings {@link SerializationSettings} settings that override global serialization settings
	 * @return serialized content in bytes
	 * @throws DocumentSerializationException thrown in case serialization fails
	 */
	public byte [] writeDocumentCollection(JSONAPIDocument<? extends Iterable<?>> documentCollection,
										   SerializationSettings serializationSettings)
			throws DocumentSerializationException {

		try {
			resourceCache.init();
			ArrayNode results = objectMapper.createArrayNode();
			Map<String, ObjectNode> includedDataMap = new HashMap<>();

			for (Object object : documentCollection.get()) {
				results.add(getDataNode(object, includedDataMap, serializationSettings));
			}

			ObjectNode result = objectMapper.createObjectNode();
			result.set(DATA, results);

			result = addIncludedSection(result, includedDataMap);

			// Handle global links and meta
			serializeMeta(documentCollection, result, serializationSettings);
			serializeLinks(documentCollection, result, serializationSettings);
			return objectMapper.writeValueAsBytes(result);
		} catch (Exception e) {
			throw new DocumentSerializationException(e);
		} finally {
			resourceCache.clear();
		}
	}


	private ObjectNode getDataNode(Object object, Map<String, ObjectNode> includedContainer,
								   SerializationSettings settings) throws IllegalAccessException {
		ObjectNode dataNode = objectMapper.createObjectNode();

		// Perform initial conversion
		ObjectNode attributesNode = objectMapper.valueToTree(object);

		// Handle id, meta and relationship fields
		String resourceId = getIdValue(object);
		
		// Remove id field from resulting attribute node
		attributesNode.remove(configuration.getIdField(object.getClass()).getName());

		// Handle meta
		Field metaField = configuration.getMetaField(object.getClass());
		if (metaField != null) {
			JsonNode meta = attributesNode.remove(metaField.getName());
			if (meta != null && shouldSerializeMeta(settings)) {
				dataNode.set(META, meta);
			}
		}

		// Handle links
		String selfHref = null;
		JsonNode jsonLinks = getResourceLinks(object, attributesNode, resourceId, settings);
		if (jsonLinks != null) {
			dataNode.set(LINKS, jsonLinks);
			
			if (jsonLinks.has(SELF)) {
				selfHref = jsonLinks.get(SELF).get(HREF).asText();
			}
		}
		
		// Handle resource identifier
		dataNode.put(TYPE, configuration.getTypeName(object.getClass()));
		if (resourceId != null) {
			dataNode.put(ID, resourceId);

			// Cache the object for recursion breaking purposes
			resourceCache.cache(resourceId.concat(configuration.getTypeName(object.getClass())), null);
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

					// In case serialisation is disabled for a given relationship, skip it
					if (!relationship.serialise()) {
						continue;
					}

					String relationshipName = relationship.value();
					
					ObjectNode relationshipDataNode = objectMapper.createObjectNode();
					relationshipsNode.set(relationshipName, relationshipDataNode);
					
					// Serialize relationship meta
					JsonNode relationshipMeta = getRelationshipMeta(object, relationshipName, settings);
					if (relationshipMeta != null) {
						relationshipDataNode.set(META, relationshipMeta);
						attributesNode.remove(configuration
								.getRelationshipMetaField(object.getClass(), relationshipName).getName());
					}
					
					// Serialize relationship links
					JsonNode relationshipLinks = getRelationshipLinks(object, relationship, selfHref, settings);
					
					if (relationshipLinks != null) {
						relationshipDataNode.set(LINKS, relationshipLinks);
						
						// Remove link object from serialized JSON
						Field refField = configuration
								.getRelationshipLinksField(object.getClass(), relationshipName);
						
						if (refField != null) {
							attributesNode.remove(refField.getName());
						}
					}
					
					if (relationshipObject instanceof Collection) {
						ArrayNode dataArrayNode = objectMapper.createArrayNode();

						for (Object element : (Collection<?>) relationshipObject) {
							String relationshipType = configuration.getTypeName(element.getClass());
							
							String idValue = getIdValue(element);

							ObjectNode identifierNode = objectMapper.createObjectNode();
							identifierNode.put(TYPE, relationshipType);
							identifierNode.put(ID, idValue);
							dataArrayNode.add(identifierNode);

							// Handle included data
							if (shouldSerializeRelationship(relationshipName, settings) && idValue != null) {
								String identifier = idValue.concat(relationshipType);
								if (!includedContainer.containsKey(identifier) && !resourceCache.contains(identifier)) {
									includedContainer.put(identifier,
											getDataNode(element, includedContainer, settings));
								}
							}
						}
						relationshipDataNode.set(DATA, dataArrayNode);

					} else {
						String relationshipType = configuration.getTypeName(relationshipObject.getClass());
						
						String idValue = getIdValue(relationshipObject);

						ObjectNode identifierNode = objectMapper.createObjectNode();
						identifierNode.put(TYPE, relationshipType);
						identifierNode.put(ID, idValue);
						
						relationshipDataNode.set(DATA, identifierNode);
						
						if (shouldSerializeRelationship(relationshipName, settings) && idValue != null) {
							String identifier = idValue.concat(relationshipType);
							if (!includedContainer.containsKey(identifier)) {
								includedContainer.put(identifier,
										getDataNode(relationshipObject, includedContainer, settings));
							}
						}
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
	 * @deprecated use writeDocumentCollection instead
	 */
	@Deprecated
	public <T> byte[] writeObjectCollection(Iterable<T> objects) throws JsonProcessingException, IllegalAccessException {
		try {
			return writeDocumentCollection(new JSONAPIDocument<>(objects));
		} catch (DocumentSerializationException e) {
			if (e.getCause() instanceof JsonProcessingException) {
				throw (JsonProcessingException) e.getCause();
			} else if (e.getCause() instanceof  IllegalAccessException) {
				throw (IllegalAccessException) e.getCause();
			}
			throw new RuntimeException(e.getCause());
		}
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

	private ObjectNode addIncludedSection(ObjectNode rootNode, Map<String, ObjectNode> includedDataMap) {
		if (!includedDataMap.isEmpty()) {
			ArrayNode includedArray = objectMapper.createArrayNode();
			includedArray.addAll(includedDataMap.values());

			rootNode.set(INCLUDED, includedArray);
		}

		return rootNode;
	}

	/**
	 * Resolves actual type to be used for resource deserialization.
	 * <p>
	 *     If user provides class with type annotation that is equal to the type value in response data, same class
	 *     will be used. If provided class is super type of actual class that is resolved using response type value,
	 *     subclass will be returned. This allows for deserializing responses in use cases where one of many subtypes
	 *     can be returned by the server and user is not sure which one will it be.
	 * </p>
	 * @param object JSON object containing type value
	 * @param userType provided user type
	 * @return {@link Class}
	 */
	private Class<?> getActualType(JsonNode object, Class<?> userType) {
		String type = object.get(TYPE).asText();

		String definedTypeName = configuration.getTypeName(userType);

		if (definedTypeName != null && definedTypeName.equals(type)) {
			return userType;
		} else {
			Class<?> actualType = configuration.getTypeClass(type);

			if (actualType != null && userType.isAssignableFrom(actualType)) {
				return actualType;
			}
		}


		throw new RuntimeException("No class was registered for type '" + type + "'.");
	}


	private Collection<?> createCollectionInstance(Class<?> type)
			throws InstantiationException, IllegalAccessException {
		if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
			return (Collection<?>) type.newInstance();
		}

		if (List.class.equals(type) || Collection.class.equals(type)) {
			return new ArrayList<>();
		}

		if (Set.class.equals(type)) {
			return new HashSet<>();
		}

		throw new RuntimeException("Unable to create appropriate instance for type: " + type.getSimpleName());
	}
	
	private JsonNode getRelationshipMeta(Object source, String relationshipName, SerializationSettings settings)
			throws IllegalAccessException {
		if (shouldSerializeMeta(settings)) {
			Field relationshipMetaField = configuration
					.getRelationshipMetaField(source.getClass(), relationshipName);
			
			if (relationshipMetaField != null && relationshipMetaField.get(source) != null) {
				return objectMapper.valueToTree(relationshipMetaField.get(source));
			}
		}
		return null;
	}
	
	private JsonNode getResourceLinks(Object resource, ObjectNode serializedResource, String resourceId,
									  SerializationSettings settings) throws IllegalAccessException {
		Type type = configuration.getType(resource.getClass());
		
		// Check if there are user-provided links
		Links links = null;
		Field linksField = configuration.getLinksField(resource.getClass());
		if (linksField != null) {
			links = (Links) linksField.get(resource);
			
			// Remove links from attributes object
			//TODO: this state change needs to be removed from here
			if (links != null) {
				serializedResource.remove(linksField.getName());
			}
		}
		
		// If enabled, handle links
		if (shouldSerializeLinks(settings)) {
			Map<String, Link> linkMap = new HashMap<>();
			
			if (links != null) {
				linkMap.putAll(links.getLinks());
			}
			
			// If link path is defined in type and id is not null and user did not explicitly set link value, create it
			if (!type.path().trim().isEmpty() && !linkMap.containsKey(SELF) && resourceId != null) {
				linkMap.put(SELF, new Link(createURL(baseURL, type.path().replace("{id}", resourceId))));
			}
			
			// If there is at least one link generated, serialize and return
			if (!linkMap.isEmpty()) {
				return objectMapper.valueToTree(new Links(linkMap)).get(LINKS);
			}
		}
		return null;
	}
	
	private JsonNode getRelationshipLinks(Object source, Relationship relationship, String ownerLink,
										  SerializationSettings settings) throws IllegalAccessException {
		if (shouldSerializeLinks(settings)) {
			Links links = null;
			
			Field relationshipLinksField = configuration
					.getRelationshipLinksField(source.getClass(), relationship.value());
			
			if (relationshipLinksField != null) {
				links = (Links) relationshipLinksField.get(source);
			}
			
			Map<String, Link> linkMap = new HashMap<>();
			
			if (links != null) {
				linkMap.putAll(links.getLinks());
			}
			
			if (!relationship.path().trim().isEmpty() && !linkMap.containsKey(SELF)) {
				linkMap.put(SELF, new Link(createURL(ownerLink, relationship.path())));
			}
			
			if (!relationship.relatedPath().trim().isEmpty() && !linkMap.containsKey(RELATED)) {
				linkMap.put(RELATED, new Link(createURL(ownerLink, relationship.relatedPath())));
			}
			
			if (!linkMap.isEmpty()) {
				return objectMapper.valueToTree(new Links(linkMap)).get(LINKS);
			}
		}
		return null;
	}
	
	private String createURL(String base, String path) {
		String result = base;
		if (!result.endsWith("/")) {
			result = result.concat("/");
		}
		
		if (path.startsWith("/")) {
			result = result.concat(path.substring(1));
		} else {
			result = result.concat(path);
		}
		
		return result;
	}
	
	private boolean shouldSerializeRelationship(String relationshipName, SerializationSettings settings) {
		if (settings != null) {
			if (settings.isRelationshipIncluded(relationshipName) && !settings.isRelationshipExcluded(relationshipName)) {
				return true;
			}
			
			if (settings.isRelationshipExcluded(relationshipName)) {
				return false;
			}
		}
		return serializationFeatures.contains(SerializationFeature.INCLUDE_RELATIONSHIP_ATTRIBUTES);
	}
	
	private boolean shouldSerializeLinks(SerializationSettings settings) {
		if (settings != null && settings.serializeLinks() != null) {
			return settings.serializeLinks();
		}
		return serializationFeatures.contains(SerializationFeature.INCLUDE_LINKS);
	}
	
	private boolean shouldSerializeMeta(SerializationSettings settings) {
		if (settings != null && settings.serializeMeta() != null) {
			return settings.serializeMeta();
		}
		return serializationFeatures.contains(SerializationFeature.INCLUDE_META);
	}
	
	/**
	 * Registers new type to be used with this converter instance.
	 * @param type {@link Class} type to register
	 * @return <code>true</code> if type was registed, else <code>false</code> (in case type was registered already or
	 * type is not eligible for registering ie. missing required annotations)
	 */
	public boolean registerType(Class<?> type) {
		if (!configuration.isRegisteredType(type) && ConverterConfiguration.isEligibleType(type)) {
			return configuration.registerType(type);
		}
		return false;
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

	/**
	 * Adds (enables) new serialization option.
	 * @param option {@link SerializationFeature} option
	 */
	public void enableSerializationOption(SerializationFeature option) {
		this.serializationFeatures.add(option);
	}

	/**
	 * Removes (disables) existing serialization option.
	 * @param option {@link SerializationFeature} feature to disable
	 */
	public void disableSerializationOption(SerializationFeature option) {
		this.serializationFeatures.remove(option);
	}
}
