package com.github.jasminb.jsonapi.abstraction;

import java.util.Iterator;
import java.util.Map;

/**
 * JSON object node interface.
 *
 * Golden Path Phase 3 - Code Generation (Diff 1)
 * Epic 1: Core JSON Abstraction Layer
 * Epic 5.5: Complete Jackson Abstraction - Enhanced for full compatibility
 */
public interface JsonObject extends JsonElement {

    // ===== FIELD ACCESS =====

    JsonElement get(String fieldName);
    boolean has(String fieldName);
    Iterator<String> fieldNames();
    int size();

    /**
     * Returns an iterator over field entries (name-value pairs).
     */
    Iterator<Map.Entry<String, JsonElement>> fields();

    /**
     * Returns true if this object has no fields.
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    // ===== FIELD MODIFICATION =====

    void put(String fieldName, String value);
    void put(String fieldName, int value);
    void put(String fieldName, long value);
    void put(String fieldName, boolean value);
    void set(String fieldName, JsonElement value);
    JsonElement remove(String fieldName);

    /**
     * Remove a field by its original name in the source object.
     * This is a convenience method for cases where field naming strategies are involved.
     */
    default JsonElement removeByFieldName(String fieldName, FieldNamingStrategy namingStrategy) {
        if (namingStrategy != null) {
            String mappedName = namingStrategy.translateName(fieldName);
            return remove(mappedName);
        }
        return remove(fieldName);
    }
}