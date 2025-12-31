package com.github.jasminb.jsonapi.abstraction;

import java.util.Collection;
import java.util.Iterator;

/**
 * JSON array node interface.
 *
 * Golden Path Phase 3 - Code Generation (Diff 1)
 * Epic 1: Core JSON Abstraction Layer
 * Epic 5.5: Complete Jackson Abstraction - Enhanced for full compatibility
 */
public interface JsonArray extends JsonElement, Iterable<JsonElement> {

    // ===== ELEMENT ACCESS =====

    int size();
    JsonElement get(int index);

    /**
     * Returns true if this array has no elements.
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    // ===== ELEMENT MODIFICATION =====

    void add(String value);
    void add(int value);
    void add(long value);
    void add(boolean value);
    void add(JsonElement value);
    JsonElement remove(int index);

    /**
     * Add all elements from a collection of JsonObjects.
     */
    default void addAll(Collection<? extends JsonObject> elements) {
        for (JsonObject element : elements) {
            add(element);
        }
    }

    /**
     * Add all elements from another JsonArray.
     */
    default void addAllElements(JsonArray other) {
        for (JsonElement element : other) {
            add(element);
        }
    }

    // ===== ITERABLE IMPLEMENTATION =====

    @Override
    default Iterator<JsonElement> iterator() {
        return new Iterator<JsonElement>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < size();
            }

            @Override
            public JsonElement next() {
                return get(index++);
            }
        };
    }
}