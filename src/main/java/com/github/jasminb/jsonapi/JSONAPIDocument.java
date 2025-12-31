package com.github.jasminb.jsonapi;

import com.github.jasminb.jsonapi.abstraction.JsonElement;
import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
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
	private JsonProcessor jsonProcessor;

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
	 * JSON API object that may hold information about the server implementation version, extensions, profiles and
	 * additional meta-data.
	 */
	private JsonApi jsonApi;

	/**
	 * Raw JSON response element
	 */
	private JsonElement responseJsonElement;


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
	 * @param data         {@link T} API resource type
	 * @param jsonProcessor {@link JsonProcessor} processor to be used for handling meta conversion
	 */
	public JSONAPIDocument(T data, JsonProcessor jsonProcessor) {
		this(data);
		this.jsonProcessor = jsonProcessor;
	}

	/**
	 * Creates new JSONAPIDocument.
	 *
	 * @param data         {@link T} API resource type
	 * @param jsonElement  {@link JsonElement} response JSON
	 * @param jsonProcessor {@link JsonProcessor} processor to be used for handling meta conversion
	 */
	public JSONAPIDocument(T data, JsonElement jsonElement, JsonProcessor jsonProcessor) {
		this(data);
		this.jsonProcessor = jsonProcessor;
		this.responseJsonElement = jsonElement;
	}

	/**
	 * Creates new JsonApiDocument.
	 *
	 * @param data  {@link T} API resource type
	 * @param links @link Links} links
	 * @param meta  {@link Map} meta
	 */
	public JSONAPIDocument(T data, Links links, Map<String, Object> meta) {
		this(data);
		this.links = links;
		this.meta = meta;
	}

	/**
	 * Creates new JsonApiDocument.
	 *
	 * @param data         {@link T} API resource type
	 * @param links        @link Links} links
	 * @param meta         {@link Map} meta
	 * @param jsonProcessor {@link JsonProcessor} processor to be used for handling meta conversion
	 */
	public JSONAPIDocument(T data, Links links, Map<String, Object> meta, JsonProcessor jsonProcessor) {
		this(data, links, meta);
		this.jsonProcessor = jsonProcessor;
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
	 * This method should be used in case error response is being built by the server side.
	 * </p>
	 *
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
	 * @param link     the link to add
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
	 *
	 * @param metaType {@link Class} target type
	 * @param <M>      type
	 * @return meta or <code>null</code>
	 */
	@Nullable
	public <M> M getMeta(Class<?> metaType) {
		if (meta != null && jsonProcessor != null) {
			return (M) jsonProcessor.convertValue(meta, metaType);
		}

		return null;
	}

	/**
	 * Returns error objects or <code>null</code> in case no errors were set.
	 *
	 * @return {@link Iterable} errors
	 */
	@Nullable
	public Iterable<? extends Error> getErrors() {
		return errors;
	}

	/**
	 * Returns raw JSON element used to create <code>this</code> {@link JSONAPIDocument}.
	 *
	 * @return {@link JsonElement}
	 */
	public JsonElement getResponseJsonElement() {
		return responseJsonElement;
	}

	/**
	 * Returns raw JSON node used to create <code>this</code> {@link JSONAPIDocument}.
	 * Only works when using Jackson as the JSON processor.
	 *
	 * @return {@link com.fasterxml.jackson.databind.JsonNode} or null if not using Jackson
	 * @deprecated Use {@link #getResponseJsonElement()} instead
	 */
	@Deprecated
	public com.fasterxml.jackson.databind.JsonNode getResponseJSONNode() {
		if (responseJsonElement instanceof com.github.jasminb.jsonapi.jackson.JacksonJsonElement) {
			return ((com.github.jasminb.jsonapi.jackson.JacksonJsonElement) responseJsonElement).getNode();
		}
		return null;
	}

	/**
	 * Returns JSON API object if present.
	 *
	 * @return {@link JsonApi} or null
	 */
	public JsonApi getJsonApi() {
		return jsonApi;
	}

	/**
	 * Sets the JSON API object.
	 *
	 * @param jsonApi {@link JsonApi}
	 */
	public void setJsonApi(JsonApi jsonApi) {
		this.jsonApi = jsonApi;
	}
}
