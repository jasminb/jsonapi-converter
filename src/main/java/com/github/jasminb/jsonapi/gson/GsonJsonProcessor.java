package com.github.jasminb.jsonapi.gson;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.abstraction.JsonElement;
import com.github.jasminb.jsonapi.abstraction.JsonObject;
import com.github.jasminb.jsonapi.abstraction.JsonArray;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Gson implementation of JsonProcessor.
 *
 * Epic 4: Alternative JSON Library Implementations - Gson Support
 * Epic 5.5: Complete Jackson Abstraction - Enhanced for full compatibility
 */
public class GsonJsonProcessor implements JsonProcessor {

    private final Gson gson;
    private static final Type MAP_TYPE = new TypeToken<HashMap<String, Object>>(){}.getType();

    public GsonJsonProcessor(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <T> T readValue(byte[] data, Class<T> clazz) {
        try {
            String json = new String(data, StandardCharsets.UTF_8);
            return gson.fromJson(json, clazz);
        } catch (JsonParseException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    @Override
    public <T> T readValue(InputStream data, Class<T> clazz) {
        try {
            return gson.fromJson(new InputStreamReader(data, StandardCharsets.UTF_8), clazz);
        } catch (JsonParseException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    @Override
    public byte[] writeValueAsBytes(Object value) {
        try {
            String json = gson.toJson(value);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    @Override
    public JsonElement parseTree(byte[] data) {
        try {
            String json = new String(data, StandardCharsets.UTF_8);
            com.google.gson.JsonElement element = gson.fromJson(json, com.google.gson.JsonElement.class);
            return GsonJsonElement.wrapElement(element);
        } catch (JsonParseException e) {
            throw new RuntimeException("Failed to parse JSON tree", e);
        }
    }

    @Override
    public JsonElement parseTree(InputStream data) {
        try {
            com.google.gson.JsonElement element = gson.fromJson(new InputStreamReader(data, StandardCharsets.UTF_8), com.google.gson.JsonElement.class);
            return GsonJsonElement.wrapElement(element);
        } catch (JsonParseException e) {
            throw new RuntimeException("Failed to parse JSON tree", e);
        }
    }

    @Override
    public <T> T treeToValue(JsonElement element, Class<T> clazz) {
        try {
            GsonJsonElement gsonElement = (GsonJsonElement) element;
            return gson.fromJson(gsonElement.getElement(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert tree to value", e);
        }
    }

    @Override
    public JsonElement valueToTree(Object value) {
        com.google.gson.JsonElement element = gson.toJsonTree(value);
        return GsonJsonElement.wrapElement(element);
    }

    @Override
    public JsonObject createObjectNode() {
        com.google.gson.JsonObject jsonObject = new com.google.gson.JsonObject();
        return new GsonJsonObject(jsonObject);
    }

    @Override
    public JsonArray createArrayNode() {
        com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
        return new GsonJsonArray(jsonArray);
    }

    @Override
    public JsonElement createTextNode(String value) {
        return new GsonJsonElement(new JsonPrimitive(value));
    }

    @Override
    public Map<String, Object> treeToMap(JsonElement element) {
        try {
            GsonJsonElement gsonElement = (GsonJsonElement) element;
            return gson.fromJson(gsonElement.getElement(), MAP_TYPE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert tree to map", e);
        }
    }

    @Override
    public <T> T convertValue(Object fromValue, Class<T> toClass) {
        // Serialize to JSON and back - this handles conversions like Map -> typed object
        String json = gson.toJson(fromValue);
        return gson.fromJson(json, toClass);
    }

    /**
     * Get the underlying Gson instance for compatibility.
     */
    public Gson getGson() {
        return gson;
    }
}