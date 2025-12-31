package com.github.jasminb.jsonapi.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.abstraction.JsonProcessorTest;
import com.github.jasminb.jsonapi.abstraction.JsonElement;
import com.github.jasminb.jsonapi.abstraction.JsonObject;
import com.github.jasminb.jsonapi.abstraction.JsonArray;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Tests for Jackson-specific JsonProcessor implementation.
 *
 * Golden Path Phase 2 - Test Scaffolding
 * Epic 3: Jackson Implementation
 */
public class JacksonJsonProcessorTest extends JsonProcessorTest {

    private ObjectMapper customObjectMapper;

    @Override
    protected JsonProcessor createJsonProcessor() {
        return new JacksonJsonProcessor(new ObjectMapper());
    }

    @Before
    public void setUpJackson() {
        customObjectMapper = new ObjectMapper();
        customObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        customObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    // ===== JACKSON-SPECIFIC FUNCTIONALITY TESTS =====

    @Test
    public void shouldSupportCustomObjectMapper() {
        // Given
        JsonProcessor processor = new JacksonJsonProcessor(customObjectMapper);

        // When
        TestPerson person = new TestPerson("SnakeCase", 25);
        byte[] json = processor.writeValueAsBytes(person);

        // Then
        String jsonString = new String(json);
        assertTrue("Should serialize with custom ObjectMapper", jsonString.length() > 0);
        assertTrue("Should contain the name", jsonString.contains("SnakeCase"));
    }

    @Test
    public void shouldMaintainObjectMapperConfiguration() {
        // Given
        ObjectMapper configuredMapper = new ObjectMapper();
        configuredMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS, true);

        JsonProcessor processor = new JacksonJsonProcessor(configuredMapper);

        // When - JSON with comments (normally invalid)
        String jsonWithComments = "{"
            + "// This is a comment\n"
            + "\"name\": \"Test\","
            + "\"age\": 30"
            + "}";

        // Then - Should parse successfully due to configuration
        TestPerson person = processor.readValue(jsonWithComments.getBytes(), TestPerson.class);
        assertNotNull("Should parse JSON with comments", person);
        assertEquals("Should extract name correctly", "Test", person.getName());
    }

    @Test
    public void shouldProvideObjectMapperAccess() {
        // Given
        ObjectMapper originalMapper = new ObjectMapper();
        JacksonJsonProcessor processor = new JacksonJsonProcessor(originalMapper);

        // When/Then
        assertSame("Should provide access to underlying ObjectMapper",
                   originalMapper, processor.getObjectMapper());
    }

    @Test
    public void shouldHandleJacksonAnnotations() {
        // Given
        JsonProcessor processor = new JacksonJsonProcessor(new ObjectMapper());

        // When
        JacksonAnnotatedClass obj = new JacksonAnnotatedClass("test", "ignored", 42);
        byte[] json = processor.writeValueAsBytes(obj);

        // Then
        String jsonString = new String(json);
        assertTrue("Should include renamed field", jsonString.contains("custom_name"));
        assertFalse("Should ignore annotated field", jsonString.contains("ignored"));
        assertTrue("Should include normal field", jsonString.contains("42"));
    }

    @Test
    public void shouldHandleComplexJsonStructures() {
        // Test Jackson's handling of complex nested structures

        // Given
        JsonProcessor processor = new JacksonJsonProcessor(new ObjectMapper());
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

    // ===== HELPER CLASSES =====

    public static class JacksonAnnotatedClass {
        @com.fasterxml.jackson.annotation.JsonProperty("custom_name")
        private String name;

        @com.fasterxml.jackson.annotation.JsonIgnore
        private String ignored;

        private int value;

        public JacksonAnnotatedClass() {}

        public JacksonAnnotatedClass(String name, String ignored, int value) {
            this.name = name;
            this.ignored = ignored;
            this.value = value;
        }

        // Getters/setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getIgnored() { return ignored; }
        public void setIgnored(String ignored) { this.ignored = ignored; }

        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}
