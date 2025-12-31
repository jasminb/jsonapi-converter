package com.github.jasminb.jsonapi.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.jasminb.jsonapi.abstraction.JsonObject;
import com.github.jasminb.jsonapi.abstraction.JsonArray;
import com.github.jasminb.jsonapi.abstraction.JsonElement;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Jackson implementation of JsonObject.
 *
 * Golden Path Phase 3 - Code Generation (Diff 2b)
 * Epic 3: Jackson Implementation
 * Epic 5.5: Complete Jackson Abstraction - Enhanced for full compatibility
 */
class JacksonJsonObject extends JacksonJsonElement implements JsonObject {
    private final ObjectNode objectNode;

    public JacksonJsonObject(ObjectNode objectNode) {
        super(objectNode);
        this.objectNode = objectNode;
    }

    @Override
    public JsonElement get(String fieldName) {
        JsonNode field = objectNode.get(fieldName);
        return field != null ? JacksonJsonProcessor.wrapNode(field) : null;
    }

    @Override
    public boolean has(String fieldName) {
        return objectNode.has(fieldName);
    }

    @Override
    public Iterator<String> fieldNames() {
        return objectNode.fieldNames();
    }

    @Override
    public Iterator<Map.Entry<String, JsonElement>> fields() {
        final Iterator<Map.Entry<String, JsonNode>> jacksonIterator = objectNode.fields();
        return new Iterator<Map.Entry<String, JsonElement>>() {
            @Override
            public boolean hasNext() {
                return jacksonIterator.hasNext();
            }

            @Override
            public Map.Entry<String, JsonElement> next() {
                Map.Entry<String, JsonNode> entry = jacksonIterator.next();
                return new AbstractMap.SimpleEntry<>(
                    entry.getKey(),
                    JacksonJsonProcessor.wrapNode(entry.getValue())
                );
            }
        };
    }

    @Override
    public int size() {
        return objectNode.size();
    }

    @Override
    public void put(String fieldName, String value) {
        objectNode.put(fieldName, value);
    }

    @Override
    public void put(String fieldName, int value) {
        objectNode.put(fieldName, value);
    }

    @Override
    public void put(String fieldName, long value) {
        objectNode.put(fieldName, value);
    }

    @Override
    public void put(String fieldName, boolean value) {
        objectNode.put(fieldName, value);
    }

    @Override
    public void set(String fieldName, JsonElement value) {
        JacksonJsonElement jacksonElement = (JacksonJsonElement) value;
        objectNode.set(fieldName, jacksonElement.getNode());
    }

    @Override
    public JsonElement remove(String fieldName) {
        JsonNode removed = objectNode.remove(fieldName);
        return removed != null ? JacksonJsonProcessor.wrapNode(removed) : null;
    }

    @Override
    public boolean isContainerNode() {
        return true;
    }

    @Override
    public boolean isValueNode() {
        return false;
    }
}

/**
 * Jackson implementation of JsonArray.
 */
class JacksonJsonArray extends JacksonJsonElement implements JsonArray {
    private final ArrayNode arrayNode;

    public JacksonJsonArray(ArrayNode arrayNode) {
        super(arrayNode);
        this.arrayNode = arrayNode;
    }

    @Override
    public int size() {
        return arrayNode.size();
    }

    @Override
    public JsonElement get(int index) {
        JsonNode element = arrayNode.get(index);
        return element != null ? JacksonJsonProcessor.wrapNode(element) : null;
    }

    @Override
    public void add(String value) {
        arrayNode.add(value);
    }

    @Override
    public void add(int value) {
        arrayNode.add(value);
    }

    @Override
    public void add(long value) {
        arrayNode.add(value);
    }

    @Override
    public void add(boolean value) {
        arrayNode.add(value);
    }

    @Override
    public void add(JsonElement value) {
        JacksonJsonElement jacksonElement = (JacksonJsonElement) value;
        arrayNode.add(jacksonElement.getNode());
    }

    @Override
    public JsonElement remove(int index) {
        JsonNode removed = arrayNode.remove(index);
        return removed != null ? JacksonJsonProcessor.wrapNode(removed) : null;
    }

    @Override
    public boolean isContainerNode() {
        return true;
    }

    @Override
    public boolean isValueNode() {
        return false;
    }
}