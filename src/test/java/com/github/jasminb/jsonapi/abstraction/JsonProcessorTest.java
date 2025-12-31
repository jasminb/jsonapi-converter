package com.github.jasminb.jsonapi.abstraction;

import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.abstraction.JsonElement;
import com.github.jasminb.jsonapi.abstraction.JsonObject;
import com.github.jasminb.jsonapi.abstraction.JsonArray;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Contract tests for JsonProcessor interface.
 * These tests define the behavior that ALL JsonProcessor implementations must satisfy.
 *
 * Following Golden Path Phase 2 - Test Scaffolding
 * Epic 1: Core JSON Abstraction Layer
 */
public abstract class JsonProcessorTest {

    protected JsonProcessor processor;

    /**
     * Subclasses must provide the JsonProcessor implementation to test
     */
    protected abstract JsonProcessor createJsonProcessor();

    @Before
    public void setUp() {
        processor = createJsonProcessor();
        assertNotNull("JsonProcessor implementation must not be null", processor);
    }

    // ===== JSON PARSING TESTS =====

    @Test
    public void shouldParseSimpleJsonObject() {
        // Given
        String json = "{\"name\":\"test\",\"value\":123}";

        // When
        JsonElement result = processor.parseTree(json.getBytes());

        // Then
        assertNotNull("Parsed tree should not be null", result);
        assertTrue("Root should be an object", result.isObject());

        JsonObject obj = result.asObject();
        assertEquals("Should parse string field correctly", "test", obj.get("name").asText());
        assertEquals("Should parse number field correctly", "123", obj.get("value").asText());
    }

    @Test
    public void shouldParseSimpleJsonArray() {
        // Given
        String json = "[\"item1\", \"item2\", 123]";

        // When
        JsonElement result = processor.parseTree(json.getBytes());

        // Then
        assertNotNull("Parsed tree should not be null", result);
        assertTrue("Root should be an array", result.isArray());

        JsonArray array = result.asArray();
        assertEquals("Array should have 3 elements", 3, array.size());
        assertEquals("First item should be 'item1'", "item1", array.get(0).asText());
        assertEquals("Second item should be 'item2'", "item2", array.get(1).asText());
        assertEquals("Third item should be '123'", "123", array.get(2).asText());
    }

    @Test
    public void shouldHandleNestedJsonStructures() {
        // Given - Simplified JSON API structure
        String json = "{"
            + "\"data\":{"
                + "\"type\":\"articles\","
                + "\"id\":\"1\","
                + "\"attributes\":{"
                    + "\"title\":\"Test Article\","
                    + "\"tags\":[\"test\",\"article\"]"
                + "}"
            + "}"
        + "}";

        // When
        JsonElement root = processor.parseTree(json.getBytes());

        // Then
        assertNotNull("Root should not be null", root);
        assertTrue("Root should be object", root.isObject());

        JsonObject data = root.asObject().get("data").asObject();
        assertEquals("Should extract type", "articles", data.get("type").asText());
        assertEquals("Should extract id", "1", data.get("id").asText());

        JsonObject attributes = data.get("attributes").asObject();
        assertEquals("Should extract title", "Test Article", attributes.get("title").asText());

        JsonArray tags = attributes.get("tags").asArray();
        assertEquals("Should have 2 tags", 2, tags.size());
        assertEquals("First tag should be 'test'", "test", tags.get(0).asText());
    }

    // ===== OBJECT MAPPING TESTS =====

    @Test
    public void shouldConvertJsonToObject() {
        // Given
        String json = "{\"name\":\"John\",\"age\":30}";

        // When
        TestPerson person = processor.readValue(json.getBytes(), TestPerson.class);

        // Then
        assertNotNull("Converted object should not be null", person);
        assertEquals("Name should be mapped correctly", "John", person.getName());
        assertEquals("Age should be mapped correctly", 30, person.getAge());
    }

    @Test
    public void shouldConvertObjectToJson() throws Exception {
        // Given
        TestPerson person = new TestPerson("Jane", 25);

        // When
        byte[] jsonBytes = processor.writeValueAsBytes(person);

        // Then
        assertNotNull("JSON bytes should not be null", jsonBytes);
        String json = new String(jsonBytes);
        assertTrue("JSON should contain name", json.contains("Jane"));
        assertTrue("JSON should contain age", json.contains("25"));
    }

    @Test
    public void shouldHandleRoundTripConversion() {
        // Given
        TestPerson original = new TestPerson("Bob", 45);

        // When
        byte[] json = processor.writeValueAsBytes(original);
        TestPerson roundTrip = processor.readValue(json, TestPerson.class);

        // Then
        assertNotNull("Round-trip object should not be null", roundTrip);
        assertEquals("Name should survive round-trip", original.getName(), roundTrip.getName());
        assertEquals("Age should survive round-trip", original.getAge(), roundTrip.getAge());
    }

    // ===== ERROR HANDLING TESTS =====

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionForInvalidJson() {
        // Given
        String invalidJson = "{invalid json}";

        // When/Then
        processor.parseTree(invalidJson.getBytes());
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionForIncompatibleType() {
        // Given
        String json = "{\"name\":\"test\"}";

        // When/Then - Try to map to incompatible type
        processor.readValue(json.getBytes(), Integer.class);
    }

    @Test
    public void shouldHandleNullInput() {
        // When/Then
        try {
            processor.parseTree((byte[]) null);
            fail("Should throw exception for null input");
        } catch (RuntimeException e) {
            // Expected
        }
    }

    // ===== TREE MANIPULATION TESTS =====

    @Test
    public void shouldCreateObjectNode() {
        // When
        JsonObject obj = processor.createObjectNode();

        // Then
        assertNotNull("Created object should not be null", obj);
        assertTrue("Created node should be object", obj.isObject());
        assertFalse("Created object should not be array", obj.isArray());
    }

    @Test
    public void shouldCreateArrayNode() {
        // When
        JsonArray array = processor.createArrayNode();

        // Then
        assertNotNull("Created array should not be null", array);
        assertTrue("Created node should be array", array.isArray());
        assertFalse("Created array should not be object", array.isObject());
        assertEquals("New array should be empty", 0, array.size());
    }

    @Test
    public void shouldConvertTreeToValue() {
        // Given
        JsonObject obj = processor.createObjectNode();
        obj.put("name", "TreeTest");
        obj.put("age", 42);

        // When
        TestPerson person = processor.treeToValue(obj, TestPerson.class);

        // Then
        assertNotNull("Converted object should not be null", person);
        assertEquals("Name should be converted", "TreeTest", person.getName());
        assertEquals("Value should be converted", 42, person.getAge());
    }

    @Test
    public void shouldConvertValueToTree() {
        // Given
        TestPerson person = new TestPerson("ValueTest", 33);

        // When
        JsonElement tree = processor.valueToTree(person);

        // Then
        assertNotNull("Tree should not be null", tree);
        assertTrue("Tree should be object", tree.isObject());

        JsonObject obj = tree.asObject();
        assertEquals("Name should be in tree", "ValueTest", obj.get("name").asText());
        assertEquals("Age should be in tree", "33", obj.get("age").asText());
    }

    // ===== HELPER CLASSES =====

    /**
     * Simple test class for object mapping validation
     */
    public static class TestPerson {
        private String name;
        private int age;

        public TestPerson() {} // Default constructor for JSON mapping

        public TestPerson(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TestPerson)) return false;
            TestPerson that = (TestPerson) o;
            return age == that.age &&
                   (name != null ? name.equals(that.name) : that.name == null);
        }
    }
}