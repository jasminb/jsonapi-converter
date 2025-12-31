package com.github.jasminb.jsonapi.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.jasminb.jsonapi.abstraction.FieldNamingStrategy;
import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.abstraction.JsonElement;
import com.github.jasminb.jsonapi.abstraction.JsonObject;
import com.github.jasminb.jsonapi.abstraction.JsonArray;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Jackson implementation of JsonProcessor.
 *
 * Golden Path Phase 3 - Code Generation (Diff 2)
 * Epic 3: Jackson Implementation
 * Epic 5.5: Complete Jackson Abstraction - Enhanced for full compatibility
 */
public class JacksonJsonProcessor implements JsonProcessor {

    private final ObjectMapper objectMapper;
    private final FieldNamingStrategy fieldNamingStrategy;

    public JacksonJsonProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.fieldNamingStrategy = createFieldNamingStrategy(objectMapper);
    }

    private static FieldNamingStrategy createFieldNamingStrategy(ObjectMapper mapper) {
        PropertyNamingStrategy jacksonStrategy = mapper.getPropertyNamingStrategy();
        if (jacksonStrategy == null) {
            return FieldNamingStrategy.IDENTITY;
        }
        // Wrap Jackson's strategy
        return fieldName -> jacksonStrategy.nameForField(null, null, fieldName);
    }

    @Override
    public <T> T readValue(byte[] data, Class<T> clazz) {
        try {
            return objectMapper.readValue(data, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    @Override
    public <T> T readValue(InputStream data, Class<T> clazz) {
        try {
            return objectMapper.readValue(data, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    @Override
    public byte[] writeValueAsBytes(Object value) {
        try {
            // If the value is a JsonElement wrapper, serialize the underlying node
            if (value instanceof JacksonJsonElement) {
                return objectMapper.writeValueAsBytes(((JacksonJsonElement) value).getNode());
            }
            return objectMapper.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    @Override
    public JsonElement parseTree(byte[] data) {
        try {
            JsonNode node = objectMapper.readTree(data);
            return wrapNode(node);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON tree", e);
        }
    }

    @Override
    public JsonElement parseTree(InputStream data) {
        try {
            JsonNode node = objectMapper.readTree(data);
            return wrapNode(node);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON tree", e);
        }
    }

    @Override
    public <T> T treeToValue(JsonElement element, Class<T> clazz) {
        try {
            JacksonJsonElement jacksonElement = (JacksonJsonElement) element;
            return objectMapper.treeToValue(jacksonElement.getNode(), clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert tree to value", e);
        }
    }

    @Override
    public JsonElement valueToTree(Object value) {
        JsonNode node = objectMapper.valueToTree(value);
        return wrapNode(node);
    }

    @Override
    public JsonObject createObjectNode() {
        ObjectNode node = objectMapper.createObjectNode();
        return new JacksonJsonObject(node);
    }

    @Override
    public JsonArray createArrayNode() {
        ArrayNode node = objectMapper.createArrayNode();
        return new JacksonJsonArray(node);
    }

    @Override
    public JsonElement createTextNode(String value) {
        return new JacksonJsonElement(new TextNode(value));
    }

    @Override
    public Map<String, Object> treeToMap(JsonElement element) {
        try {
            JacksonJsonElement jacksonElement = (JacksonJsonElement) element;
            JsonParser p = objectMapper.treeAsTokens(jacksonElement.getNode());
            MapType mapType = TypeFactory.defaultInstance()
                    .constructMapType(HashMap.class, String.class, Object.class);
            return objectMapper.readValue(p, mapType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert tree to map", e);
        }
    }

    @Override
    public <T> T convertValue(Object fromValue, Class<T> toClass) {
        return objectMapper.convertValue(fromValue, toClass);
    }

    @Override
    public FieldNamingStrategy getFieldNamingStrategy() {
        return fieldNamingStrategy;
    }

    /**
     * Get the underlying ObjectMapper for compatibility.
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Wrap a Jackson JsonNode in the appropriate abstraction type.
     * This method is public to allow backward compatibility with code that
     * needs to convert between Jackson JsonNode and the abstraction layer.
     *
     * @param node the Jackson JsonNode to wrap
     * @return the wrapped JsonElement, or null if node is null
     */
    public static JsonElement wrapNode(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            return new JacksonJsonObject((ObjectNode) node);
        }
        if (node.isArray()) {
            return new JacksonJsonArray((ArrayNode) node);
        }
        return new JacksonJsonElement(node);
    }
}