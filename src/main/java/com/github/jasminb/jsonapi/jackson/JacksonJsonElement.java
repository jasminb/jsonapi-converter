package com.github.jasminb.jsonapi.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.jasminb.jsonapi.abstraction.JsonElement;
import com.github.jasminb.jsonapi.abstraction.JsonObject;
import com.github.jasminb.jsonapi.abstraction.JsonArray;

/**
 * Jackson implementation of JsonElement.
 */
public class JacksonJsonElement implements JsonElement {
    protected final JsonNode node;

    public JacksonJsonElement(JsonNode node) {
        this.node = node;
    }

    public JsonNode getNode() {
        return node;
    }

    /**
     * Compatibility method for accessing the underlying Jackson JsonNode.
     */
    public JsonNode getUnderlyingNode() {
        return node;
    }

    @Override
    public boolean isObject() {
        return node.isObject();
    }

    @Override
    public boolean isArray() {
        return node.isArray();
    }

    @Override
    public boolean isNull() {
        return node.isNull();
    }

    @Override
    public boolean isTextual() {
        return node.isTextual();
    }

    @Override
    public boolean isNumber() {
        return node.isNumber();
    }

    @Override
    public boolean isContainerNode() {
        return node.isContainerNode();
    }

    @Override
    public boolean isValueNode() {
        return node.isValueNode();
    }

    @Override
    public String asText() {
        return node.asText();
    }

    @Override
    public String asText(String defaultValue) {
        return node.asText(defaultValue);
    }

    @Override
    public int asInt() {
        return node.asInt();
    }

    @Override
    public int asInt(int defaultValue) {
        return node.asInt(defaultValue);
    }

    @Override
    public long asLong() {
        return node.asLong();
    }

    @Override
    public long asLong(long defaultValue) {
        return node.asLong(defaultValue);
    }

    @Override
    public JsonObject asObject() {
        if (!isObject()) {
            throw new IllegalStateException("Not an object node");
        }
        return new JacksonJsonObject((ObjectNode) node);
    }

    @Override
    public JsonArray asArray() {
        if (!isArray()) {
            throw new IllegalStateException("Not an array node");
        }
        return new JacksonJsonArray((ArrayNode) node);
    }
}
