package com.github.jasminb.jsonapi.gson;

import com.google.gson.JsonPrimitive;
import com.github.jasminb.jsonapi.abstraction.JsonObject;
import com.github.jasminb.jsonapi.abstraction.JsonArray;
import com.github.jasminb.jsonapi.abstraction.JsonElement;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Gson implementation of JsonElement.
 *
 * Epic 4: Alternative JSON Library Implementations - Gson Support
 * Epic 5.5: Complete Jackson Abstraction - Enhanced for full compatibility
 */
class GsonJsonElement implements JsonElement {
    protected final com.google.gson.JsonElement element;

    public GsonJsonElement(com.google.gson.JsonElement element) {
        this.element = element;
    }

    public com.google.gson.JsonElement getElement() {
        return element;
    }

    @Override
    public boolean isObject() {
        return element.isJsonObject();
    }

    @Override
    public boolean isArray() {
        return element.isJsonArray();
    }

    @Override
    public boolean isNull() {
        return element.isJsonNull();
    }

    @Override
    public boolean isTextual() {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    @Override
    public boolean isNumber() {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
    }

    @Override
    public boolean isContainerNode() {
        return element.isJsonObject() || element.isJsonArray();
    }

    @Override
    public boolean isValueNode() {
        return element.isJsonPrimitive() || element.isJsonNull();
    }

    @Override
    public String asText() {
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return element.toString();
    }

    @Override
    public String asText(String defaultValue) {
        try {
            return asText();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public int asInt() {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return primitive.getAsInt();
            }
            // Try to parse string as number
            try {
                return Integer.parseInt(primitive.getAsString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    public int asInt(int defaultValue) {
        try {
            return asInt();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public long asLong() {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return primitive.getAsLong();
            }
            // Try to parse string as number
            try {
                return Long.parseLong(primitive.getAsString());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    @Override
    public long asLong(long defaultValue) {
        try {
            return asLong();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public JsonObject asObject() {
        if (!isObject()) {
            throw new IllegalStateException("Not an object node");
        }
        return new GsonJsonObject(element.getAsJsonObject());
    }

    @Override
    public JsonArray asArray() {
        if (!isArray()) {
            throw new IllegalStateException("Not an array node");
        }
        return new GsonJsonArray(element.getAsJsonArray());
    }

    /**
     * Wrap a Gson JsonElement in the appropriate abstraction type.
     */
    static JsonElement wrapElement(com.google.gson.JsonElement element) {
        if (element == null) {
            return null;
        }
        if (element.isJsonObject()) {
            return new GsonJsonObject(element.getAsJsonObject());
        }
        if (element.isJsonArray()) {
            return new GsonJsonArray(element.getAsJsonArray());
        }
        return new GsonJsonElement(element);
    }
}

/**
 * Gson implementation of JsonObject.
 */
class GsonJsonObject extends GsonJsonElement implements JsonObject {
    private final com.google.gson.JsonObject jsonObject;

    public GsonJsonObject(com.google.gson.JsonObject jsonObject) {
        super(jsonObject);
        this.jsonObject = jsonObject;
    }

    @Override
    public JsonElement get(String fieldName) {
        com.google.gson.JsonElement element = jsonObject.get(fieldName);
        return element != null ? GsonJsonElement.wrapElement(element) : null;
    }

    @Override
    public boolean has(String fieldName) {
        return jsonObject.has(fieldName);
    }

    @Override
    public Iterator<String> fieldNames() {
        return jsonObject.keySet().iterator();
    }

    @Override
    public Iterator<Map.Entry<String, JsonElement>> fields() {
        final Iterator<Map.Entry<String, com.google.gson.JsonElement>> gsonIterator =
            jsonObject.entrySet().iterator();
        return new Iterator<Map.Entry<String, JsonElement>>() {
            @Override
            public boolean hasNext() {
                return gsonIterator.hasNext();
            }

            @Override
            public Map.Entry<String, JsonElement> next() {
                Map.Entry<String, com.google.gson.JsonElement> entry = gsonIterator.next();
                return new AbstractMap.SimpleEntry<>(
                    entry.getKey(),
                    GsonJsonElement.wrapElement(entry.getValue())
                );
            }
        };
    }

    @Override
    public int size() {
        return jsonObject.size();
    }

    @Override
    public void put(String fieldName, String value) {
        jsonObject.addProperty(fieldName, value);
    }

    @Override
    public void put(String fieldName, int value) {
        jsonObject.addProperty(fieldName, value);
    }

    @Override
    public void put(String fieldName, long value) {
        jsonObject.addProperty(fieldName, value);
    }

    @Override
    public void put(String fieldName, boolean value) {
        jsonObject.addProperty(fieldName, value);
    }

    @Override
    public void set(String fieldName, JsonElement value) {
        GsonJsonElement gsonElement = (GsonJsonElement) value;
        jsonObject.add(fieldName, gsonElement.getElement());
    }

    @Override
    public JsonElement remove(String fieldName) {
        com.google.gson.JsonElement removed = jsonObject.remove(fieldName);
        return removed != null ? GsonJsonElement.wrapElement(removed) : null;
    }
}

/**
 * Gson implementation of JsonArray.
 */
class GsonJsonArray extends GsonJsonElement implements JsonArray {
    private final com.google.gson.JsonArray jsonArray;

    public GsonJsonArray(com.google.gson.JsonArray jsonArray) {
        super(jsonArray);
        this.jsonArray = jsonArray;
    }

    @Override
    public int size() {
        return jsonArray.size();
    }

    @Override
    public JsonElement get(int index) {
        if (index >= 0 && index < jsonArray.size()) {
            com.google.gson.JsonElement element = jsonArray.get(index);
            return GsonJsonElement.wrapElement(element);
        }
        return null;
    }

    @Override
    public void add(String value) {
        jsonArray.add(value);
    }

    @Override
    public void add(int value) {
        jsonArray.add(value);
    }

    @Override
    public void add(long value) {
        jsonArray.add(value);
    }

    @Override
    public void add(boolean value) {
        jsonArray.add(value);
    }

    @Override
    public void add(JsonElement value) {
        GsonJsonElement gsonElement = (GsonJsonElement) value;
        jsonArray.add(gsonElement.getElement());
    }

    @Override
    public JsonElement remove(int index) {
        if (index >= 0 && index < jsonArray.size()) {
            com.google.gson.JsonElement removed = jsonArray.remove(index);
            return GsonJsonElement.wrapElement(removed);
        }
        return null;
    }
}
