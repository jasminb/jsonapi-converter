package com.github.jasminb.jsonapi.abstraction;

import java.util.Iterator;
import java.util.Map;

/**
 * Base interface for JSON tree elements.
 *
 * Golden Path Phase 3 - Code Generation (Diff 1)
 * Epic 1: Core JSON Abstraction Layer
 * Epic 5.5: Complete Jackson Abstraction - Enhanced for full compatibility
 */
public interface JsonElement extends Iterable<JsonElement> {

    // ===== TYPE CHECKING =====

    boolean isObject();
    boolean isArray();
    boolean isNull();
    boolean isTextual();
    boolean isNumber();

    /**
     * Returns true if this is a container node (object or array).
     */
    boolean isContainerNode();

    /**
     * Returns true if this is a value node (string, number, boolean, null).
     */
    boolean isValueNode();

    // ===== VALUE ACCESS =====

    String asText();
    String asText(String defaultValue);
    int asInt();
    int asInt(int defaultValue);
    long asLong();
    long asLong(long defaultValue);

    // ===== TYPE CONVERSION =====

    JsonObject asObject();
    JsonArray asArray();

    // ===== ARRAY ELEMENT ACCESS (convenience methods) =====

    /**
     * Get array element by index. Returns null if this is not an array or index is out of bounds.
     */
    default JsonElement get(int index) {
        if (isArray()) {
            return asArray().get(index);
        }
        return null;
    }

    /**
     * Returns iterator for array elements. Returns empty iterator if not an array.
     */
    @Override
    default Iterator<JsonElement> iterator() {
        if (isArray()) {
            return asArray().iterator();
        }
        return java.util.Collections.emptyIterator();
    }

    // ===== OBJECT FIELD ACCESS (convenience methods) =====

    /**
     * Get a field value by name. Returns null if this is not an object or field doesn't exist.
     */
    default JsonElement get(String fieldName) {
        if (isObject()) {
            return asObject().get(fieldName);
        }
        return null;
    }

    /**
     * Check if field exists. Returns false if this is not an object.
     */
    default boolean has(String fieldName) {
        if (isObject()) {
            return asObject().has(fieldName);
        }
        return false;
    }

    /**
     * Check if field exists and is not null.
     */
    default boolean hasNonNull(String fieldName) {
        if (isObject()) {
            JsonElement elem = asObject().get(fieldName);
            return elem != null && !elem.isNull();
        }
        return false;
    }

    /**
     * Get iterator of field names. Returns empty iterator if not an object.
     */
    default Iterator<String> fieldNames() {
        if (isObject()) {
            return asObject().fieldNames();
        }
        return java.util.Collections.emptyIterator();
    }

    /**
     * Get iterator of field entries (name -> value). Returns empty iterator if not an object.
     */
    default Iterator<Map.Entry<String, JsonElement>> fields() {
        if (isObject()) {
            return asObject().fields();
        }
        return java.util.Collections.emptyIterator();
    }
}