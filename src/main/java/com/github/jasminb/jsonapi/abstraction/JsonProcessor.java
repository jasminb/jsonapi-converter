package com.github.jasminb.jsonapi.abstraction;

import java.io.InputStream;
import java.util.Map;

/**
 * Core abstraction for JSON processing operations.
 *
 * This interface provides a unified API for JSON parsing, tree navigation,
 * and object mapping that can be implemented by different JSON libraries
 * (Jackson, Gson, JSON-B, etc.).
 *
 * Golden Path Phase 3 - Code Generation (Diff 1)
 * Epic 1: Core JSON Abstraction Layer
 * Epic 5.5: Complete Jackson Abstraction - Enhanced for full compatibility
 */
public interface JsonProcessor {

    // ===== OBJECT MAPPING =====

    /**
     * Parse JSON from byte array and convert to specified type.
     */
    <T> T readValue(byte[] data, Class<T> clazz);

    /**
     * Parse JSON from InputStream and convert to specified type.
     */
    <T> T readValue(InputStream data, Class<T> clazz);

    /**
     * Convert object to JSON byte array.
     */
    byte[] writeValueAsBytes(Object value);

    // ===== TREE MODEL =====

    /**
     * Parse JSON from byte array into tree structure.
     */
    JsonElement parseTree(byte[] data);

    /**
     * Parse JSON from InputStream into tree structure.
     */
    JsonElement parseTree(InputStream data);

    /**
     * Convert tree element to specified type.
     */
    <T> T treeToValue(JsonElement element, Class<T> clazz);

    /**
     * Convert object to tree element.
     */
    JsonElement valueToTree(Object value);

    // ===== TREE CREATION =====

    /**
     * Create new empty object node.
     */
    JsonObject createObjectNode();

    /**
     * Create new empty array node.
     */
    JsonArray createArrayNode();

    /**
     * Create a text (string) node.
     */
    JsonElement createTextNode(String value);

    // ===== MAP CONVERSION =====

    /**
     * Convert a JsonElement to a Map (for meta objects).
     * The resulting map has String keys and Object values.
     */
    Map<String, Object> treeToMap(JsonElement element);

    /**
     * Convert an object to another type using this processor's settings.
     * Useful for converting Map to typed object.
     */
    <T> T convertValue(Object fromValue, Class<T> toClass);

    // ===== CONFIGURATION =====

    /**
     * Get the field naming strategy used by this processor.
     * Returns IDENTITY if no custom strategy is configured.
     */
    default FieldNamingStrategy getFieldNamingStrategy() {
        return FieldNamingStrategy.IDENTITY;
    }
}