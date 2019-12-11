package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.models.errors.Error;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON API Document wrapper.
 *
 * @param <T> the type parameter
 * @author jbegic
 */
public class JSONAPIDocument<T> {
	private T data;
	private ObjectMapper deserializer;

	private Iterable<? extends Error> errors;

	/**
	 * Top level response link object.
	 */
	private Links links;

	/**
	 * A map of meta fields, keyed by the meta field name
	 */
	private Map<String, Object> meta;

	/**
	 * Raw JSON-node response
	 */
	private JsonNode responseJSONNode;


	/**
	 * Creates new JsonApiDocument.
	 *
	 * @param data {@link T} API resource type
	 */
	public JSONAPIDocument(T data) {
		this.data = data;
	}

	/**
	 * Creates new JSONAPIDocument.
	 *
	 * @param data {@link T} API resource type
	 * @param deserializer {@link ObjectMapper} deserializer to be used for handling meta conversion
	 */
	public JSONAPIDocument(T data, ObjectMapper deserializer) {
		this(data);
		this.deserializer = deserializer;
	}

	/**
	 * Creates new JSONAPIDocument.
	 *
	 * @param data {@link T} API resource type
	 * @param jsonNode {@link JsonNode} response JSON
	 * @param deserializer {@link ObjectMapper} deserializer to be used for handling meta conversion
	 */
	public JSONAPIDocument(T data, JsonNode jsonNode, ObjectMapper deserializer) {
		this(data);
		this.deserializer = deserializer;
		this.responseJSONNode = jsonNode;
	}

	/**
	 * Creates new JsonApiDocument.
	 *
	 * @param data {@link T} API resource type
	 * @param links @link Links} links
	 * @param meta {@link Map} meta
	 */
	public JSONAPIDocument(T data, Links links, Map<String, Object> meta) {
		this(data);
		this.links = links;
		this.meta = meta;
	}

	/**
	 * Creates new JsonApiDocument.
	 *
	 * @param data {@link T} API resource type
	 * @param links @link Links} links
	 * @param meta {@link Map} meta
	 * @param deserializer {@link ObjectMapper} deserializer to be used for handling meta conversion
	 */
	public JSONAPIDocument(T data, Links links, Map<String, Object> meta, ObjectMapper deserializer) {
		this(data, links, meta);
		this.deserializer = deserializer;
	}

	/**
	 * Creates new JSONAPIDocument.
	 */
	public JSONAPIDocument() {
		// Default constructor
	}

	/**
	 * Creates new JSONAPIDocument.
	 *
	 * @param errors errors
	 */
	public JSONAPIDocument(Iterable<? extends Error> errors) {
		this.errors = errors;
	}

	/**
	 * Creates new JSONAPIDocument.
	 *
	 * @param error error
	 */
	public JSONAPIDocument(Error error) {
		this.errors = Arrays.asList(error);
	}

	/**
	 * Factory method for creating JSONAPIDocument that holds the Error object.
	 *
	 * <p>
	 *     This method should be used in case error response is being built by the server side.
	 * </p>
	 * @param errors
	 */
	@NotNull
	public static JSONAPIDocument<?> createErrorDocument(Iterable<? extends Error> errors) {
		JSONAPIDocument<?> result = new JSONAPIDocument();
		result.errors = errors;
		return result;
	}

	/**
	 * Gets resource object
	 *
	 * @return {@link T} resource object
	 */
	@Nullable
	public T get() {
		return data;
	}

	/**
	 * Get meta data.
	 *
	 * @return {@link Map} meta
	 */
	@Nullable
	public Map<String, ?> getMeta() {
		return meta;
	}

	/**
	 * Sets meta data.
	 *
	 * @param meta {@link Map} meta
	 */
	public void setMeta(Map<String, ?> meta) {
		this.meta = new HashMap<>(meta);
	}

	public void addMeta(String key, Object value) {
		if (meta == null) {
			meta = new HashMap<>();
		}
		meta.put(key, value);
	}

	/**
	 * Gets links.
	 *
	 * @return the links
	 */
	@Nullable
	public Links getLinks() {
		return links;
	}

	/**
	 * Adds a named link.
	 *
	 * @param linkName the named link to add
	 * @param link the link to add
	 */
	public void addLink(String linkName, Link link) {
		if (links == null) {
			links = new Links(new HashMap<String, Link>());
		}
		links.addLink(linkName, link);
	}

	/**
	 * Sets links.
	 *
	 * @param links the links
	 */
	public void setLinks(Links links) {
		this.links = links;
	}

	/**
	 * Returns typed meta-data object or <code>null</code> if no meta is present.
	 * @param metaType {@link Class} target type
	 * @param <M> type
	 * @return meta or <code>null</code>
	 */
	@Nullable
	public <M> M getMeta(Class<?> metaType) {
		if (meta != null && deserializer != null) {
			return (M) deserializer.convertValue(meta, metaType);
		}

		return null;
	}

	/**
	 * Returns error objects or <code>null</code> in case no errors were set.
	 * @return {@link Iterable} errors
	 */
	@Nullable
	public Iterable<? extends Error> getErrors() {
		return errors;
	}

	/**
	 * Returns raw JSON node used to create <code>this</code> {@link JSONAPIDocument}.
	 *
	 * @return {@link JsonNode}
	 */
	public JsonNode getResponseJSONNode() {
		return responseJSONNode;
	}
}
