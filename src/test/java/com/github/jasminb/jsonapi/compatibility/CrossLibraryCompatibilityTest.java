package com.github.jasminb.jsonapi.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import jakarta.json.bind.JsonbBuilder;
import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.abstraction.JsonElement;
import com.github.jasminb.jsonapi.abstraction.JsonObject;
import com.github.jasminb.jsonapi.abstraction.JsonArray;
import com.github.jasminb.jsonapi.jackson.JacksonJsonProcessor;
import com.github.jasminb.jsonapi.gson.GsonJsonProcessor;
import com.github.jasminb.jsonapi.jsonb.JsonBJsonProcessor;
import com.github.jasminb.jsonapi.discovery.JsonProcessorFactory;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Cross-library compatibility tests for all JSON processors.
 *
 * Epic 4: Alternative JSON Library Implementations - Cross-Library Compatibility
 */
@RunWith(Parameterized.class)
public class CrossLibraryCompatibilityTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"Jackson", createJacksonProcessor()},
            {"Gson", createGsonProcessor()},
            {"JSON-B", createJsonBProcessor()}
        });
    }

    private static JsonProcessor createJacksonProcessor() {
        try {
            return new JacksonJsonProcessor(new ObjectMapper());
        } catch (Exception e) {
            return null; // Not available
        }
    }

    private static JsonProcessor createGsonProcessor() {
        try {
            return new GsonJsonProcessor(new Gson());
        } catch (Exception e) {
            return null; // Not available
        }
    }

    private static JsonProcessor createJsonBProcessor() {
        try {
            return new JsonBJsonProcessor(JsonbBuilder.create());
        } catch (Exception e) {
            return null; // Not available
        }
    }

    private final String processorName;
    private final JsonProcessor processor;

    public CrossLibraryCompatibilityTest(String processorName, JsonProcessor processor) {
        this.processorName = processorName;
        this.processor = processor;
    }

    @Before
    public void setUp() {
        org.junit.Assume.assumeNotNull("Processor not available: " + processorName, processor);
    }

    // ===== BASIC JSON OPERATIONS =====

    @Test
    public void shouldParseAndSerializeSimpleObjects() {
        // Given
        TestObject original = new TestObject("test", 42, true);

        // When
        byte[] json = processor.writeValueAsBytes(original);
        TestObject roundTrip = processor.readValue(json, TestObject.class);

        // Then
        assertEquals("Name should survive round-trip", original.getName(), roundTrip.getName());
        assertEquals("Age should survive round-trip", original.getAge(), roundTrip.getAge());
        assertEquals("Flag should survive round-trip", original.isFlag(), roundTrip.isFlag());
    }

    @Test
    public void shouldHandleJsonTreeOperations() {
        // Given
        String json = "{"
            + "\"name\": \"TestTree\","
            + "\"values\": [1, 2, 3],"
            + "\"nested\": {"
                + "\"key\": \"value\""
            + "}"
        + "}";

        // When
        JsonElement root = processor.parseTree(json.getBytes());

        // Then
        assertTrue("Root should be object", root.isObject());

        JsonObject obj = root.asObject();
        assertEquals("Should extract name", "TestTree", obj.get("name").asText());

        JsonArray values = obj.get("values").asArray();
        assertEquals("Should have 3 values", 3, values.size());
        assertEquals("First value should be 1", 1, values.get(0).asInt());

        JsonObject nested = obj.get("nested").asObject();
        assertEquals("Should extract nested value", "value", nested.get("key").asText());
    }

    @Test
    public void shouldCreateAndManipulateNodes() {
        // When
        JsonObject obj = processor.createObjectNode();
        JsonArray arr = processor.createArrayNode();

        // Then - Test node creation
        assertTrue("Created object should be object", obj.isObject());
        assertTrue("Created array should be array", arr.isArray());
        assertEquals("Object should start empty", 0, obj.size());
        assertEquals("Array should start empty", 0, arr.size());

        // Test manipulation (skip for JSON-B as it's immutable)
        if (!processorName.equals("JSON-B")) {
            obj.put("test", "value");
            arr.add("item");

            assertEquals("Object should accept values", "value", obj.get("test").asText());
            assertEquals("Array should accept values", "item", arr.get(0).asText());
        }
    }

    @Test
    public void shouldHandleTreeToValueConversion() {
        // Given
        JsonObject obj = processor.createObjectNode();

        // Skip mutation tests for JSON-B
        if (!processorName.equals("JSON-B")) {
            obj.put("name", "TreeConversion");
            obj.put("age", 25);

            // When
            TestObject converted = processor.treeToValue(obj, TestObject.class);

            // Then
            assertEquals("Name should convert correctly", "TreeConversion", converted.getName());
            assertEquals("Age should convert correctly", 25, converted.getAge());
        }
    }

    @Test
    public void shouldHandleValueToTreeConversion() {
        // Given
        TestObject obj = new TestObject("ValueToTree", 30, false);

        // When
        JsonElement tree = processor.valueToTree(obj);

        // Then
        assertTrue("Tree should be object", tree.isObject());
        JsonObject jsonObj = tree.asObject();
        assertEquals("Name should be in tree", "ValueToTree", jsonObj.get("name").asText());
        assertEquals("Age should be in tree", 30, jsonObj.get("age").asInt());
    }

    // ===== JSON API SPECIFIC TESTS =====

    @Test
    public void shouldHandleJsonApiStructure() {
        // Test with a simplified JSON API structure

        // Given
        String jsonApiDocument = "{"
            + "\"data\": {"
                + "\"type\": \"articles\","
                + "\"id\": \"1\","
                + "\"attributes\": {"
                    + "\"title\": \"Cross Library Test\","
                    + "\"content\": \"Testing all libraries\""
                + "}"
            + "},"
            + "\"included\": []"
        + "}";

        // When
        JsonElement root = processor.parseTree(jsonApiDocument.getBytes());

        // Then
        assertTrue("Root should be object", root.isObject());

        JsonObject data = root.asObject().get("data").asObject();
        assertEquals("Type should be articles", "articles", data.get("type").asText());
        assertEquals("ID should be 1", "1", data.get("id").asText());

        JsonObject attributes = data.get("attributes").asObject();
        assertEquals("Title should match", "Cross Library Test", attributes.get("title").asText());
        assertEquals("Content should match", "Testing all libraries", attributes.get("content").asText());
    }

    @Test
    public void shouldHandleNumberTypes() {
        // Given
        String json = "{"
            + "\"intValue\": 42,"
            + "\"longValue\": 1234567890123,"
            + "\"stringNumber\": \"456\""
        + "}";

        // When
        JsonElement root = processor.parseTree(json.getBytes());
        JsonObject obj = root.asObject();

        // Then
        assertEquals("Integer should parse correctly", 42, obj.get("intValue").asInt());
        assertEquals("Long should parse correctly", 1234567890123L, obj.get("longValue").asLong());
        assertEquals("String number should convert", 456, obj.get("stringNumber").asInt());
    }

    @Test
    public void shouldHandleNullValues() {
        // Given
        String json = "{"
            + "\"nullValue\": null,"
            + "\"stringValue\": \"not null\""
        + "}";

        // When
        JsonElement root = processor.parseTree(json.getBytes());
        JsonObject obj = root.asObject();

        // Then
        assertTrue("Null value should be recognized", obj.get("nullValue").isNull());
        assertFalse("String value should not be null", obj.get("stringValue").isNull());
        assertEquals("String value should be extracted", "not null", obj.get("stringValue").asText());
    }

    // ===== SERVICE DISCOVERY TESTS =====

    @Test
    public void shouldBeDiscoverableByFactory() {
        // This test validates that our implementations are properly registered

        // When
        JsonProcessor discoveredProcessor = JsonProcessorFactory.createDefault();

        // Then
        assertNotNull("Factory should discover a processor", discoveredProcessor);

        // Test basic functionality
        TestObject obj = new TestObject("Discovery", 99, true);
        byte[] json = discoveredProcessor.writeValueAsBytes(obj);
        TestObject roundTrip = discoveredProcessor.readValue(json, TestObject.class);

        assertEquals("Discovered processor should work", obj.getName(), roundTrip.getName());
    }

    // ===== HELPER CLASSES =====

    public static class TestObject {
        private String name;
        private int age;
        private boolean flag;

        public TestObject() {}

        public TestObject(String name, int age, boolean flag) {
            this.name = name;
            this.age = age;
            this.flag = flag;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }

        public boolean isFlag() { return flag; }
        public void setFlag(boolean flag) { this.flag = flag; }
    }
}