package com.github.jasminb.jsonapi.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.Expose;
import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.abstraction.JsonProcessorTest;
import com.github.jasminb.jsonapi.abstraction.JsonElement;
import com.github.jasminb.jsonapi.abstraction.JsonObject;
import com.github.jasminb.jsonapi.abstraction.JsonArray;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Tests for Gson-specific JsonProcessor implementation.
 *
 * Epic 4: Alternative JSON Library Implementations - Gson Support
 */
public class GsonJsonProcessorTest extends JsonProcessorTest {

    private Gson customGson;

    @Override
    protected JsonProcessor createJsonProcessor() {
        return new GsonJsonProcessor(new Gson());
    }

    @Before
    public void setUpGson() {
        customGson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .serializeNulls()
            .create();
    }

    // ===== GSON-SPECIFIC FUNCTIONALITY TESTS =====

    @Test
    public void shouldSupportCustomGson() {
        // Given
        JsonProcessor processor = new GsonJsonProcessor(customGson);

        // When
        TestPerson person = new TestPerson("UnderscoreCase", 25);
        byte[] json = processor.writeValueAsBytes(person);

        // Then
        String jsonString = new String(json);
        assertTrue("Should serialize with custom Gson", jsonString.length() > 0);
        assertTrue("Should contain the name", jsonString.contains("UnderscoreCase"));
        // Gson with LOWER_CASE_WITH_UNDERSCORES would convert fieldNames
    }

    @Test
    public void shouldMaintainGsonConfiguration() {
        // Given
        Gson configuredGson = new GsonBuilder()
            .serializeNulls()  // Include null values
            .create();

        JsonProcessor processor = new GsonJsonProcessor(configuredGson);

        // When
        TestPersonWithNull person = new TestPersonWithNull("Test", null);
        byte[] json = processor.writeValueAsBytes(person);

        // Then
        String jsonString = new String(json);
        assertTrue("Should include null values due to configuration",
                  jsonString.contains("null"));
    }

    @Test
    public void shouldProvideGsonAccess() {
        // Given
        Gson originalGson = new Gson();
        GsonJsonProcessor processor = new GsonJsonProcessor(originalGson);

        // When/Then
        assertSame("Should provide access to underlying Gson",
                   originalGson, processor.getGson());
    }

    @Test
    public void shouldHandleGsonAnnotations() {
        // Given - Configure Gson to respect @Expose annotations
        Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();
        JsonProcessor processor = new GsonJsonProcessor(gson);

        // When
        GsonAnnotatedClass obj = new GsonAnnotatedClass("test", "exposed", "ignored");
        byte[] json = processor.writeValueAsBytes(obj);

        // Then
        String jsonString = new String(json);
        assertTrue("Should include renamed field", jsonString.contains("custom_name"));
        assertTrue("Should include exposed field", jsonString.contains("exposed"));
        assertFalse("Should ignore non-exposed field", jsonString.contains("ignored"));
    }

    @Test
    public void shouldHandleComplexJsonStructures() {
        // Test Gson's handling of complex nested structures

        // Given
        JsonProcessor processor = new GsonJsonProcessor(new Gson());
        String complexJson = "{"
            + "\"data\": {"
                + "\"type\": \"articles\","
                + "\"id\": \"1\","
                + "\"attributes\": {"
                    + "\"title\": \"Complex Article\","
                    + "\"tags\": [\"java\", \"json\", \"api\"]"
                + "}"
            + "}"
        + "}";

        // When
        JsonElement root = processor.parseTree(complexJson.getBytes());

        // Then
        assertTrue("Root should be object", root.isObject());

        JsonObject data = root.asObject().get("data").asObject();
        assertEquals("Should extract type", "articles", data.get("type").asText());

        JsonObject attributes = data.get("attributes").asObject();
        JsonArray tags = attributes.get("tags").asArray();

        assertEquals("Should handle nested arrays", 3, tags.size());
        assertEquals("Should extract nested array values", "java", tags.get(0).asText());
    }

    @Test
    public void shouldHandleNumbersCorrectly() {
        // Gson handles numbers differently than Jackson, let's test this

        // Given
        JsonProcessor processor = new GsonJsonProcessor(new Gson());
        String json = "{\"intValue\": 42, \"longValue\": 1234567890123, \"stringNumber\": \"123\"}";

        // When
        JsonElement root = processor.parseTree(json.getBytes());
        JsonObject obj = root.asObject();

        // Then
        assertEquals("Should handle integer", 42, obj.get("intValue").asInt());
        assertEquals("Should handle long", 1234567890123L, obj.get("longValue").asLong());
        assertEquals("Should parse string number", 123, obj.get("stringNumber").asInt());
    }

    @Test
    public void shouldCreateEmptyNodesCorrectly() {
        // Test Gson's node creation capabilities

        // Given
        JsonProcessor processor = new GsonJsonProcessor(new Gson());

        // When
        JsonObject obj = processor.createObjectNode();
        JsonArray arr = processor.createArrayNode();

        // Then
        assertTrue("Created object should be object", obj.isObject());
        assertEquals("Object should be empty", 0, obj.size());

        assertTrue("Created array should be array", arr.isArray());
        assertEquals("Array should be empty", 0, arr.size());

        // Test modification
        obj.put("test", "value");
        arr.add("item");

        assertEquals("Object should accept values", "value", obj.get("test").asText());
        assertEquals("Array should accept values", "item", arr.get(0).asText());
    }

    // ===== HELPER CLASSES =====

    public static class TestPersonWithNull {
        private String name;
        private String description;

        public TestPersonWithNull() {}

        public TestPersonWithNull(String name, String description) {
            this.name = name;
            this.description = description;
        }

        // Getters/setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class GsonAnnotatedClass {
        @SerializedName("custom_name")
        @Expose
        private String name;

        @Expose
        private String exposedField;

        private String ignoredField; // Not exposed, so will be ignored

        public GsonAnnotatedClass() {}

        public GsonAnnotatedClass(String name, String exposedField, String ignoredField) {
            this.name = name;
            this.exposedField = exposedField;
            this.ignoredField = ignoredField;
        }

        // Getters/setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getExposedField() { return exposedField; }
        public void setExposedField(String exposedField) { this.exposedField = exposedField; }

        public String getIgnoredField() { return ignoredField; }
        public void setIgnoredField(String ignoredField) { this.ignoredField = ignoredField; }
    }
}