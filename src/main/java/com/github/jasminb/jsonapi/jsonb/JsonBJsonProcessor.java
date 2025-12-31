package com.github.jasminb.jsonapi.jsonb;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
import jakarta.json.*;
import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.abstraction.JsonElement;
import com.github.jasminb.jsonapi.abstraction.JsonObject;
import com.github.jasminb.jsonapi.abstraction.JsonArray;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON-B implementation of JsonProcessor.
 *
 * Epic 4: Alternative JSON Library Implementations - JSON-B Support
 * Epic 5.5: Complete Jackson Abstraction - Enhanced for full compatibility
 */
public class JsonBJsonProcessor implements JsonProcessor {

    private final Jsonb jsonb;
    private final JsonBuilderFactory jsonBuilderFactory;
    private final JsonReaderFactory jsonReaderFactory;

    public JsonBJsonProcessor(Jsonb jsonb) {
        this.jsonb = jsonb;
        this.jsonBuilderFactory = Json.createBuilderFactory(null);
        this.jsonReaderFactory = Json.createReaderFactory(null);
    }

    @Override
    public <T> T readValue(byte[] data, Class<T> clazz) {
        try {
            String json = new String(data, StandardCharsets.UTF_8);
            return jsonb.fromJson(json, clazz);
        } catch (JsonbException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    @Override
    public <T> T readValue(InputStream data, Class<T> clazz) {
        try {
            return jsonb.fromJson(data, clazz);
        } catch (JsonbException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    @Override
    public byte[] writeValueAsBytes(Object value) {
        try {
            String json = jsonb.toJson(value);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (JsonbException e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    @Override
    public JsonElement parseTree(byte[] data) {
        try {
            String json = new String(data, StandardCharsets.UTF_8);
            JsonReader reader = jsonReaderFactory.createReader(new StringReader(json));
            JsonValue value = reader.readValue();
            return JsonBJsonElement.wrapValue(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON tree", e);
        }
    }

    @Override
    public JsonElement parseTree(InputStream data) {
        try {
            JsonReader reader = jsonReaderFactory.createReader(data);
            JsonValue value = reader.readValue();
            return JsonBJsonElement.wrapValue(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON tree", e);
        }
    }

    @Override
    public <T> T treeToValue(JsonElement element, Class<T> clazz) {
        try {
            JsonBJsonElement jsonbElement = (JsonBJsonElement) element;
            String json = jsonbElement.getValue().toString();
            return jsonb.fromJson(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert tree to value", e);
        }
    }

    @Override
    public JsonElement valueToTree(Object value) {
        try {
            String json = jsonb.toJson(value);
            JsonReader reader = jsonReaderFactory.createReader(new StringReader(json));
            JsonValue jsonValue = reader.readValue();
            return JsonBJsonElement.wrapValue(jsonValue);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert value to tree", e);
        }
    }

    @Override
    public JsonObject createObjectNode() {
        return new JsonBMutableJsonObject(jsonBuilderFactory);
    }

    @Override
    public JsonArray createArrayNode() {
        return new JsonBMutableJsonArray(jsonBuilderFactory);
    }

    @Override
    public JsonElement createTextNode(String value) {
        return new JsonBJsonElement(Json.createValue(value));
    }

    @Override
    public Map<String, Object> treeToMap(JsonElement element) {
        try {
            JsonBJsonElement jsonbElement = (JsonBJsonElement) element;
            String json = jsonbElement.getValue().toString();
            // Use JSON-B to deserialize to a map
            return jsonb.fromJson(json, new HashMap<String, Object>().getClass());
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert tree to map", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object fromValue, Class<T> toClass) {
        // Serialize to JSON and back - this handles conversions like Map -> typed object
        String json = jsonb.toJson(fromValue);
        return jsonb.fromJson(json, toClass);
    }

    /**
     * Get the underlying Jsonb instance for compatibility.
     */
    public Jsonb getJsonb() {
        return jsonb;
    }

    /**
     * Get the JsonBuilderFactory for creating JSON structures.
     */
    public JsonBuilderFactory getJsonBuilderFactory() {
        return jsonBuilderFactory;
    }
}