package com.github.jasminb.jsonapi.jsonb;

import jakarta.json.*;
import com.github.jasminb.jsonapi.abstraction.JsonObject;
import com.github.jasminb.jsonapi.abstraction.JsonArray;
import com.github.jasminb.jsonapi.abstraction.JsonElement;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;

/**
 * JSON-B implementation of JsonElement.
 *
 * Epic 4: Alternative JSON Library Implementations - JSON-B Support
 * Epic 5.5: Complete Jackson Abstraction - Enhanced for full compatibility
 */
class JsonBJsonElement implements JsonElement {
    protected final JsonValue value;
    protected final JsonBuilderFactory builderFactory;

    public JsonBJsonElement(JsonValue value) {
        this.value = value;
        this.builderFactory = Json.createBuilderFactory(null);
    }

    public JsonValue getValue() {
        return value;
    }

    @Override
    public boolean isObject() {
        return value.getValueType() == JsonValue.ValueType.OBJECT;
    }

    @Override
    public boolean isArray() {
        return value.getValueType() == JsonValue.ValueType.ARRAY;
    }

    @Override
    public boolean isNull() {
        return value.getValueType() == JsonValue.ValueType.NULL;
    }

    @Override
    public boolean isTextual() {
        return value.getValueType() == JsonValue.ValueType.STRING;
    }

    @Override
    public boolean isNumber() {
        return value.getValueType() == JsonValue.ValueType.NUMBER;
    }

    @Override
    public boolean isContainerNode() {
        return isObject() || isArray();
    }

    @Override
    public boolean isValueNode() {
        JsonValue.ValueType type = value.getValueType();
        return type == JsonValue.ValueType.STRING ||
               type == JsonValue.ValueType.NUMBER ||
               type == JsonValue.ValueType.TRUE ||
               type == JsonValue.ValueType.FALSE ||
               type == JsonValue.ValueType.NULL;
    }

    @Override
    public String asText() {
        switch (value.getValueType()) {
            case STRING:
                return ((JsonString) value).getString();
            case NUMBER:
                return ((JsonNumber) value).toString();
            case TRUE:
                return "true";
            case FALSE:
                return "false";
            case NULL:
                return "null";
            default:
                return value.toString();
        }
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
        switch (value.getValueType()) {
            case NUMBER:
                return ((JsonNumber) value).intValue();
            case STRING:
                try {
                    return Integer.parseInt(((JsonString) value).getString());
                } catch (NumberFormatException e) {
                    return 0;
                }
            default:
                return 0;
        }
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
        switch (value.getValueType()) {
            case NUMBER:
                return ((JsonNumber) value).longValue();
            case STRING:
                try {
                    return Long.parseLong(((JsonString) value).getString());
                } catch (NumberFormatException e) {
                    return 0L;
                }
            default:
                return 0L;
        }
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
        return new JsonBJsonObject((jakarta.json.JsonObject) value);
    }

    @Override
    public JsonArray asArray() {
        if (!isArray()) {
            throw new IllegalStateException("Not an array node");
        }
        return new JsonBJsonArray((jakarta.json.JsonArray) value);
    }

    /**
     * Wrap a JSON-B JsonValue in the appropriate abstraction type.
     */
    static JsonElement wrapValue(JsonValue value) {
        if (value == null) {
            return null;
        }
        if (value.getValueType() == JsonValue.ValueType.OBJECT) {
            return new JsonBJsonObject((jakarta.json.JsonObject) value);
        }
        if (value.getValueType() == JsonValue.ValueType.ARRAY) {
            return new JsonBJsonArray((jakarta.json.JsonArray) value);
        }
        return new JsonBJsonElement(value);
    }
}

/**
 * JSON-B implementation of JsonObject.
 */
class JsonBJsonObject extends JsonBJsonElement implements JsonObject {
    private final jakarta.json.JsonObject jsonObject;

    public JsonBJsonObject(jakarta.json.JsonObject jsonObject) {
        super(jsonObject);
        this.jsonObject = jsonObject;
    }

    @Override
    public JsonElement get(String fieldName) {
        JsonValue value = jsonObject.get(fieldName);
        return value != null ? JsonBJsonElement.wrapValue(value) : null;
    }

    @Override
    public boolean has(String fieldName) {
        return jsonObject.containsKey(fieldName);
    }

    @Override
    public Iterator<String> fieldNames() {
        return jsonObject.keySet().iterator();
    }

    @Override
    public Iterator<Map.Entry<String, JsonElement>> fields() {
        final Iterator<Map.Entry<String, JsonValue>> jsonbIterator =
            jsonObject.entrySet().iterator();
        return new Iterator<Map.Entry<String, JsonElement>>() {
            @Override
            public boolean hasNext() {
                return jsonbIterator.hasNext();
            }

            @Override
            public Map.Entry<String, JsonElement> next() {
                Map.Entry<String, JsonValue> entry = jsonbIterator.next();
                return new AbstractMap.SimpleEntry<>(
                    entry.getKey(),
                    JsonBJsonElement.wrapValue(entry.getValue())
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
        // JSON-B JsonObject is immutable, so we need to create a new builder
        // This is a limitation of JSON-B compared to Jackson/Gson
        throw new UnsupportedOperationException(
            "JSON-B JsonObject is immutable. Use JsonObjectBuilder for mutations.");
    }

    @Override
    public void put(String fieldName, int value) {
        throw new UnsupportedOperationException(
            "JSON-B JsonObject is immutable. Use JsonObjectBuilder for mutations.");
    }

    @Override
    public void put(String fieldName, long value) {
        throw new UnsupportedOperationException(
            "JSON-B JsonObject is immutable. Use JsonObjectBuilder for mutations.");
    }

    @Override
    public void put(String fieldName, boolean value) {
        throw new UnsupportedOperationException(
            "JSON-B JsonObject is immutable. Use JsonObjectBuilder for mutations.");
    }

    @Override
    public void set(String fieldName, JsonElement value) {
        throw new UnsupportedOperationException(
            "JSON-B JsonObject is immutable. Use JsonObjectBuilder for mutations.");
    }

    @Override
    public JsonElement remove(String fieldName) {
        throw new UnsupportedOperationException(
            "JSON-B JsonObject is immutable. Use JsonObjectBuilder for mutations.");
    }
}

/**
 * JSON-B implementation of JsonArray.
 */
class JsonBJsonArray extends JsonBJsonElement implements JsonArray {
    private final jakarta.json.JsonArray jsonArray;

    public JsonBJsonArray(jakarta.json.JsonArray jsonArray) {
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
            JsonValue value = jsonArray.get(index);
            return JsonBJsonElement.wrapValue(value);
        }
        return null;
    }

    @Override
    public void add(String value) {
        // JSON-B JsonArray is immutable, similar to JsonObject
        throw new UnsupportedOperationException(
            "JSON-B JsonArray is immutable. Use JsonArrayBuilder for mutations.");
    }

    @Override
    public void add(int value) {
        throw new UnsupportedOperationException(
            "JSON-B JsonArray is immutable. Use JsonArrayBuilder for mutations.");
    }

    @Override
    public void add(long value) {
        throw new UnsupportedOperationException(
            "JSON-B JsonArray is immutable. Use JsonArrayBuilder for mutations.");
    }

    @Override
    public void add(boolean value) {
        throw new UnsupportedOperationException(
            "JSON-B JsonArray is immutable. Use JsonArrayBuilder for mutations.");
    }

    @Override
    public void add(JsonElement value) {
        throw new UnsupportedOperationException(
            "JSON-B JsonArray is immutable. Use JsonArrayBuilder for mutations.");
    }

    @Override
    public JsonElement remove(int index) {
        throw new UnsupportedOperationException(
            "JSON-B JsonArray is immutable. Use JsonArrayBuilder for mutations.");
    }
}

/**
 * Mutable JSON-B implementation of JsonObject using builder pattern.
 * This provides mutability compatibility with Jackson/Gson implementations.
 */
class JsonBMutableJsonObject extends JsonBJsonElement implements JsonObject {
    private JsonObjectBuilder builder;
    private jakarta.json.JsonObject currentObject;

    public JsonBMutableJsonObject(JsonBuilderFactory factory) {
        super(Json.createObjectBuilder().build());
        this.builder = factory.createObjectBuilder();
        this.currentObject = builder.build();
    }

    private void rebuild() {
        currentObject = builder.build();
    }

    @Override
    public JsonValue getValue() {
        return currentObject;
    }

    @Override
    public JsonElement get(String fieldName) {
        JsonValue value = currentObject.get(fieldName);
        return value != null ? JsonBJsonElement.wrapValue(value) : null;
    }

    @Override
    public boolean has(String fieldName) {
        return currentObject.containsKey(fieldName);
    }

    @Override
    public Iterator<String> fieldNames() {
        return currentObject.keySet().iterator();
    }

    @Override
    public Iterator<Map.Entry<String, JsonElement>> fields() {
        final Iterator<Map.Entry<String, JsonValue>> jsonbIterator =
            currentObject.entrySet().iterator();
        return new Iterator<Map.Entry<String, JsonElement>>() {
            @Override
            public boolean hasNext() {
                return jsonbIterator.hasNext();
            }

            @Override
            public Map.Entry<String, JsonElement> next() {
                Map.Entry<String, JsonValue> entry = jsonbIterator.next();
                return new AbstractMap.SimpleEntry<>(
                    entry.getKey(),
                    JsonBJsonElement.wrapValue(entry.getValue())
                );
            }
        };
    }

    @Override
    public int size() {
        return currentObject.size();
    }

    @Override
    public void put(String fieldName, String value) {
        builder.add(fieldName, value);
        rebuild();
    }

    @Override
    public void put(String fieldName, int value) {
        builder.add(fieldName, value);
        rebuild();
    }

    @Override
    public void put(String fieldName, long value) {
        builder.add(fieldName, value);
        rebuild();
    }

    @Override
    public void put(String fieldName, boolean value) {
        builder.add(fieldName, value);
        rebuild();
    }

    @Override
    public void set(String fieldName, JsonElement value) {
        JsonBJsonElement jsonbElement = (JsonBJsonElement) value;
        builder.add(fieldName, jsonbElement.getValue());
        rebuild();
    }

    @Override
    public JsonElement remove(String fieldName) {
        JsonElement removed = get(fieldName);
        builder.remove(fieldName);
        rebuild();
        return removed;
    }
}

/**
 * Mutable JSON-B implementation of JsonArray using builder pattern.
 */
class JsonBMutableJsonArray extends JsonBJsonElement implements JsonArray {
    private JsonArrayBuilder builder;
    private jakarta.json.JsonArray currentArray;

    public JsonBMutableJsonArray(JsonBuilderFactory factory) {
        super(Json.createArrayBuilder().build());
        this.builder = factory.createArrayBuilder();
        this.currentArray = builder.build();
    }

    private void rebuild() {
        currentArray = builder.build();
    }

    @Override
    public JsonValue getValue() {
        return currentArray;
    }

    @Override
    public int size() {
        return currentArray.size();
    }

    @Override
    public JsonElement get(int index) {
        if (index >= 0 && index < currentArray.size()) {
            JsonValue value = currentArray.get(index);
            return JsonBJsonElement.wrapValue(value);
        }
        return null;
    }

    @Override
    public void add(String value) {
        builder.add(value);
        rebuild();
    }

    @Override
    public void add(int value) {
        builder.add(value);
        rebuild();
    }

    @Override
    public void add(long value) {
        builder.add(value);
        rebuild();
    }

    @Override
    public void add(boolean value) {
        builder.add(value);
        rebuild();
    }

    @Override
    public void add(JsonElement value) {
        JsonBJsonElement jsonbElement = (JsonBJsonElement) value;
        builder.add(jsonbElement.getValue());
        rebuild();
    }

    @Override
    public JsonElement remove(int index) {
        // JSON-B doesn't have array element removal, this would be complex to implement
        throw new UnsupportedOperationException(
            "JSON-B JsonArray doesn't support element removal.");
    }
}
