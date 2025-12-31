package com.github.jasminb.jsonapi.discovery;

import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.abstraction.JsonElement;
import com.github.jasminb.jsonapi.abstraction.JsonObject;
import com.github.jasminb.jsonapi.abstraction.JsonArray;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;

/**
 * Tests for JsonProcessor service discovery mechanism.
 *
 * Following Golden Path Phase 2 - Test Scaffolding
 * Epic 2: Service Discovery Framework
 */
public class JsonProcessorFactoryTest {

    @Before
    public void setUp() {
        // Clear any cached processors for clean test state
        JsonProcessorFactory.clearCache();
    }

    @After
    public void tearDown() {
        JsonProcessorFactory.resetDiscovery();
    }

    // ===== AUTO-DISCOVERY TESTS =====

    @Test
    public void shouldAutoDetectAvailableProcessors() {
        // When
        JsonProcessor processor = JsonProcessorFactory.createDefault();

        // Then
        assertNotNull("Should create a processor", processor);
        // Note: Actual implementation will depend on classpath
    }

    @Test
    public void shouldReturnJacksonWhenAvailable() {
        // Given - Jackson should be available in this test environment

        // When
        JsonProcessor processor = JsonProcessorFactory.createDefault();

        // Then
        assertNotNull("Should create Jackson processor", processor);
        assertTrue("Should be Jackson implementation",
                  processor.getClass().getName().contains("Jackson"));
    }

    @Test
    public void shouldThrowExceptionWhenNoProcessorsAvailable() {
        // Given
        JsonProcessorFactory.setAvailableProviders(Collections.<JsonProcessorProvider>emptyList());

        // When/Then
        try {
            JsonProcessorFactory.createDefault();
            fail("Should throw exception when no processors available");
        } catch (RuntimeException e) {
            assertTrue("Should mention no processors found",
                      e.getMessage().toLowerCase().contains("no json processor"));
        }
    }

    // ===== EXPLICIT SELECTION TESTS =====

    @Test
    public void shouldCreateSpecificProcessor() {
        // When
        JsonProcessor jacksonProcessor = JsonProcessorFactory.create("jackson");

        // Then
        assertNotNull("Should create specific processor", jacksonProcessor);
        assertTrue("Should be Jackson implementation",
                  jacksonProcessor.getClass().getName().contains("Jackson"));
    }

    @Test
    public void shouldThrowExceptionForUnknownProcessor() {
        // When/Then
        try {
            JsonProcessorFactory.create("unknown-library");
            fail("Should throw exception for unknown processor");
        } catch (RuntimeException e) {
            assertTrue("Should mention unknown processor",
                      e.getMessage().toLowerCase().contains("unknown"));
        }
    }

    // ===== PROVIDER PRIORITY TESTS =====

    @Test
    public void shouldRespectProviderPriority() {
        // Given
        MockProvider highPriority = new MockProvider("high", 100, true);
        MockProvider lowPriority = new MockProvider("low", 50, true);

        JsonProcessorFactory.setAvailableProviders(Arrays.<JsonProcessorProvider>asList(lowPriority, highPriority));

        // When
        JsonProcessor processor = JsonProcessorFactory.createDefault();

        // Then
        assertNotNull("Should create processor", processor);
        assertEquals("Should use high priority provider", "high",
                    ((MockJsonProcessor) processor).getName());
    }

    @Test
    public void shouldSkipUnavailableProviders() {
        // Given
        MockProvider unavailable = new MockProvider("unavailable", 100, false);
        MockProvider available = new MockProvider("available", 50, true);

        JsonProcessorFactory.setAvailableProviders(Arrays.<JsonProcessorProvider>asList(unavailable, available));

        // When
        JsonProcessor processor = JsonProcessorFactory.createDefault();

        // Then
        assertNotNull("Should create processor", processor);
        assertEquals("Should use available provider", "available",
                    ((MockJsonProcessor) processor).getName());
    }

    // ===== CONFIGURATION TESTS =====

    @Test
    public void shouldCreateProcessorWithConfig() {
        // Given
        JsonProcessorConfig config = JsonProcessorConfig.builder()
            .fieldNamingStrategy(FieldNamingStrategy.SNAKE_CASE)
            .serializationInclusion(SerializationInclusion.NON_NULL)
            .build();

        // When
        JsonProcessor processor = JsonProcessorFactory.create("jackson", config);

        // Then
        assertNotNull("Should create configured processor", processor);
        // Note: Specific configuration validation will be in implementation tests
    }

    @Test
    public void shouldCacheProcessorInstances() {
        // When
        JsonProcessor first = JsonProcessorFactory.createDefault();
        JsonProcessor second = JsonProcessorFactory.createDefault();

        // Then
        assertSame("Should return same cached instance", first, second);
    }

    @Test
    public void shouldNotCacheConfiguredProcessors() {
        // Given
        JsonProcessorConfig config1 = JsonProcessorConfig.builder()
            .fieldNamingStrategy(FieldNamingStrategy.CAMEL_CASE)
            .build();
        JsonProcessorConfig config2 = JsonProcessorConfig.builder()
            .fieldNamingStrategy(FieldNamingStrategy.SNAKE_CASE)
            .build();

        // When
        JsonProcessor first = JsonProcessorFactory.create("jackson", config1);
        JsonProcessor second = JsonProcessorFactory.create("jackson", config2);

        // Then
        assertNotSame("Should create different instances for different configs", first, second);
    }

    // ===== DIAGNOSTICS TESTS =====

    @Test
    public void shouldProvideAvailableProcessorsList() {
        // When
        List<String> available = JsonProcessorFactory.getAvailableProcessors();

        // Then
        assertNotNull("Should return list of available processors", available);
        assertFalse("Should have at least one processor", available.isEmpty());
        assertTrue("Should include Jackson", available.contains("jackson"));
    }

    @Test
    public void shouldProvideDetailedDiagnostics() {
        // When
        JsonProcessorDiagnostics diagnostics = JsonProcessorFactory.getDiagnostics();

        // Then
        assertNotNull("Should provide diagnostics", diagnostics);
        assertNotNull("Should list detected providers", diagnostics.getDetectedProviders());
        assertNotNull("Should show selected provider", diagnostics.getSelectedProvider());
        assertNotNull("Should report classpath info", diagnostics.getClasspathInfo());
    }

    // ===== MOCK CLASSES FOR TESTING =====

    private static class MockProvider implements JsonProcessorProvider {
        private final String name;
        private final int priority;
        private final boolean available;

        public MockProvider(String name, int priority, boolean available) {
            this.name = name;
            this.priority = priority;
            this.available = available;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public JsonProcessor create() {
            return new MockJsonProcessor(name);
        }

        @Override
        public JsonProcessor create(JsonProcessorConfig config) {
            return new MockJsonProcessor(name + "-configured");
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return name + " mock provider";
        }

        @Override
        public String getVersion() {
            return "1.0.0-mock";
        }
    }

    private static class MockJsonProcessor implements JsonProcessor {
        private final String name;

        public MockJsonProcessor(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        // Minimal implementation for testing
        @Override
        public <T> T readValue(byte[] data, Class<T> clazz) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public <T> T readValue(java.io.InputStream data, Class<T> clazz) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public byte[] writeValueAsBytes(Object value) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public JsonElement parseTree(byte[] data) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public JsonElement parseTree(java.io.InputStream data) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public <T> T treeToValue(JsonElement element, Class<T> clazz) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public JsonElement valueToTree(Object value) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public JsonObject createObjectNode() {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public JsonArray createArrayNode() {
            throw new UnsupportedOperationException("Mock implementation");
        }
    }
}