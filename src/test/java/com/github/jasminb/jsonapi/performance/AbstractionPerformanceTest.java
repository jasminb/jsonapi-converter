package com.github.jasminb.jsonapi.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.ResourceConverter;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.models.Article;
import com.github.jasminb.jsonapi.models.Author;
import com.github.jasminb.jsonapi.models.Comment;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Ignore;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Performance baseline and regression tests.
 *
 * Golden Path Phase 2 - Test Scaffolding
 * Critical: Ensure abstraction layer doesn't degrade performance >5%
 */
public class AbstractionPerformanceTest {

    private ResourceConverter currentConverter;
    private ObjectMapper directObjectMapper;

    // Test data
    private byte[] simpleJsonApiDocument;
    private byte[] complexJsonApiDocument;
    private byte[] collectionJsonApiDocument;

    // Performance thresholds (in milliseconds)
    private static final long SIMPLE_PARSE_THRESHOLD_MS = 50;
    private static final long COMPLEX_PARSE_THRESHOLD_MS = 200;
    private static final long COLLECTION_PARSE_THRESHOLD_MS = 500;
    private static final double MAX_PERFORMANCE_DEGRADATION = 0.05; // 5%

    @Before
    public void setUp() {
        // Initialize current Jackson-based converter
        currentConverter = new ResourceConverter(Article.class, Author.class, Comment.class);
        directObjectMapper = new ObjectMapper();

        // Prepare test data
        setupTestData();
    }

    @After
    public void tearDown() {
        // Clean up any resources
    }

    // ===== CURRENT PERFORMANCE BASELINE =====

    @Test
    public void shouldBenchmarkCurrentJacksonPerformance() {
        // This test establishes our baseline performance metrics

        // Warm up JVM
        warmUpJvm();

        // Benchmark simple document parsing
        long simpleParseTime = benchmarkSimpleDocumentParsing();

        // Benchmark complex document parsing
        long complexParseTime = benchmarkComplexDocumentParsing();

        // Benchmark collection parsing
        long collectionParseTime = benchmarkCollectionParsing();

        // Benchmark serialization
        long serializationTime = benchmarkSerialization();

        // Record baseline metrics for comparison
        recordBaseline(simpleParseTime, complexParseTime, collectionParseTime, serializationTime);

        // Assert performance is within acceptable bounds
        assertTrue("Simple parsing should be fast", simpleParseTime < SIMPLE_PARSE_THRESHOLD_MS);
        assertTrue("Complex parsing should be reasonable", complexParseTime < COMPLEX_PARSE_THRESHOLD_MS);
        assertTrue("Collection parsing should be reasonable", collectionParseTime < COLLECTION_PARSE_THRESHOLD_MS);

        System.out.println("=== BASELINE PERFORMANCE METRICS ===");
        System.out.println("Simple document parsing: " + simpleParseTime + "ms");
        System.out.println("Complex document parsing: " + complexParseTime + "ms");
        System.out.println("Collection parsing: " + collectionParseTime + "ms");
        System.out.println("Serialization: " + serializationTime + "ms");
    }

    @Test
    @Ignore("Will be enabled in Phase 4 when abstraction is implemented")
    public void shouldCompareAbstractionPerformance() {
        // This test will compare abstracted JsonProcessor vs direct Jackson
        // Implementation pending Phase 3

        // Given
        JsonProcessor abstractedProcessor = null; // Will be injected in Phase 3

        // When - TBD in Phase 4
        // long abstractedTime = benchmarkAbstractedProcessor(abstractedProcessor);
        // long directTime = benchmarkDirectJackson();

        // Then - TBD in Phase 4
        // double degradation = (double)(abstractedTime - directTime) / directTime;
        // assertTrue("Performance degradation should be < 5%",
        //           degradation < MAX_PERFORMANCE_DEGRADATION);
    }

    @Test
    @Ignore("Will be enabled in Phase 5 for cross-library comparison")
    public void shouldCompareDifferentJsonLibraries() {
        // This test will compare Jackson vs Gson vs JSON-B performance
        // Implementation pending Phase 4-5
    }

    // ===== MEMORY USAGE TESTS =====

    @Test
    public void shouldBenchmarkMemoryUsage() {
        // Measure memory usage for baseline comparison

        Runtime runtime = Runtime.getRuntime();

        // Force GC to get clean baseline
        System.gc();
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();

        // Parse documents and measure memory growth
        List<Article> articles = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            try {
                JSONAPIDocument<Article> doc = currentConverter.readDocument(simpleJsonApiDocument, Article.class);
                articles.add(doc.get());
            } catch (Exception e) {
                fail("Should not fail during memory benchmark: " + e.getMessage());
            }
        }

        long afterParsingMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryGrowth = afterParsingMemory - baselineMemory;

        System.out.println("Memory growth for 1000 simple documents: " +
                          (memoryGrowth / 1024) + "KB");

        // Sanity check - shouldn't use excessive memory
        assertTrue("Memory usage should be reasonable", memoryGrowth < 50 * 1024 * 1024); // 50MB
    }

    // ===== STRESS TESTS =====

    @Test
    public void shouldHandleLargeDocuments() {
        // Test performance with large JSON API documents

        long startTime = System.currentTimeMillis();

        try {
            JSONAPIDocument<List<Article>> doc =
                currentConverter.readDocumentCollection(collectionJsonApiDocument, Article.class);

            assertNotNull("Should parse large document", doc);
            assertNotNull("Should have data", doc.get());
            assertTrue("Should have multiple articles", doc.get().size() > 100);

        } catch (Exception e) {
            fail("Should handle large documents: " + e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        assertTrue("Large document parsing should complete in reasonable time",
                  duration < 5000); // 5 seconds
    }

    @Test
    public void shouldHandleConcurrentParsing() throws InterruptedException {
        // Test performance under concurrent load

        final int threadCount = 10;
        final int iterationsPerThread = 100;
        final List<Exception> exceptions = new ArrayList<>();

        Thread[] threads = new Thread[threadCount];
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        try {
                            currentConverter.readDocument(simpleJsonApiDocument, Article.class);
                        } catch (Exception e) {
                            synchronized (exceptions) {
                                exceptions.add(e);
                            }
                        }
                    }
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        long duration = System.currentTimeMillis() - startTime;

        assertTrue("Should handle concurrent parsing without errors", exceptions.isEmpty());
        assertTrue("Concurrent parsing should complete in reasonable time",
                  duration < 10000); // 10 seconds

        System.out.println("Concurrent parsing (" + threadCount + " threads, " +
                          iterationsPerThread + " iterations each): " + duration + "ms");
    }

    // ===== HELPER METHODS =====

    private void warmUpJvm() {
        // Warm up JVM for more accurate benchmarks
        for (int i = 0; i < 100; i++) {
            try {
                currentConverter.readDocument(simpleJsonApiDocument, Article.class);
            } catch (Exception e) {
                // Ignore warm-up errors
            }
        }
    }

    private long benchmarkSimpleDocumentParsing() {
        final int iterations = 1000;
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            try {
                currentConverter.readDocument(simpleJsonApiDocument, Article.class);
            } catch (Exception e) {
                fail("Benchmark failed: " + e.getMessage());
            }
        }

        long duration = System.nanoTime() - startTime;
        return TimeUnit.NANOSECONDS.toMillis(duration) / iterations;
    }

    private long benchmarkComplexDocumentParsing() {
        final int iterations = 100;
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            try {
                currentConverter.readDocument(complexJsonApiDocument, Article.class);
            } catch (Exception e) {
                fail("Complex benchmark failed: " + e.getMessage());
            }
        }

        long duration = System.nanoTime() - startTime;
        return TimeUnit.NANOSECONDS.toMillis(duration) / iterations;
    }

    private long benchmarkCollectionParsing() {
        final int iterations = 50;
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            try {
                currentConverter.readDocumentCollection(collectionJsonApiDocument, Article.class);
            } catch (Exception e) {
                fail("Collection benchmark failed: " + e.getMessage());
            }
        }

        long duration = System.nanoTime() - startTime;
        return TimeUnit.NANOSECONDS.toMillis(duration) / iterations;
    }

    private long benchmarkSerialization() {
        final int iterations = 1000;

        // Create test article
        Article article = new Article();
        article.setId("test-id");
        article.setTitle("Performance Test Article");

        JSONAPIDocument<Article> document = new JSONAPIDocument<>(article);

        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            try {
                currentConverter.writeDocument(document);
            } catch (Exception e) {
                fail("Serialization benchmark failed: " + e.getMessage());
            }
        }

        long duration = System.nanoTime() - startTime;
        return TimeUnit.NANOSECONDS.toMillis(duration) / iterations;
    }

    private void setupTestData() {
        // Simple JSON API document
        simpleJsonApiDocument = ("{"
            + "\"data\":{"
                + "\"type\":\"articles\","
                + "\"id\":\"1\","
                + "\"attributes\":{"
                    + "\"title\":\"Test Article\""
                + "}"
            + "}"
        + "}").getBytes();

        // Complex JSON API document with relationships
        complexJsonApiDocument = ("{"
            + "\"data\":{"
                + "\"type\":\"articles\","
                + "\"id\":\"1\","
                + "\"attributes\":{"
                    + "\"title\":\"Complex Test Article\""
                + "},"
                + "\"relationships\":{"
                    + "\"author\":{"
                        + "\"data\":{\"type\":\"people\",\"id\":\"1\"}"
                    + "},"
                    + "\"comments\":{"
                        + "\"data\":["
                            + "{\"type\":\"comments\",\"id\":\"1\"},"
                            + "{\"type\":\"comments\",\"id\":\"2\"}"
                        + "]"
                    + "}"
                + "}"
            + "},"
            + "\"included\":["
                + "{"
                    + "\"type\":\"people\","
                    + "\"id\":\"1\","
                    + "\"attributes\":{"
                        + "\"firstName\":\"Test\","
                        + "\"lastName\":\"Author\""
                    + "}"
                + "},"
                + "{"
                    + "\"type\":\"comments\","
                    + "\"id\":\"1\","
                    + "\"attributes\":{"
                        + "\"body\":\"Great article!\""
                    + "}"
                + "},"
                + "{"
                    + "\"type\":\"comments\","
                    + "\"id\":\"2\","
                    + "\"attributes\":{"
                        + "\"body\":\"Very informative.\""
                    + "}"
                + "}"
            + "]"
        + "}").getBytes();

        // Collection document
        StringBuilder collectionJson = new StringBuilder();
        collectionJson.append("{\"data\":[");
        for (int i = 1; i <= 500; i++) {
            if (i > 1) collectionJson.append(",");
            collectionJson.append("{"
                + "\"type\":\"articles\","
                + "\"id\":\"").append(i).append("\","
                + "\"attributes\":{"
                    + "\"title\":\"Article ").append(i).append("\""
                + "}"
            + "}");
        }
        collectionJson.append("]}");
        collectionJsonApiDocument = collectionJson.toString().getBytes();
    }

    private void recordBaseline(long simpleParseTime, long complexParseTime,
                              long collectionParseTime, long serializationTime) {
        // Store baseline metrics for future comparison
        // This could write to a file or system property for later reference
        System.setProperty("jsonapi.baseline.simple", String.valueOf(simpleParseTime));
        System.setProperty("jsonapi.baseline.complex", String.valueOf(complexParseTime));
        System.setProperty("jsonapi.baseline.collection", String.valueOf(collectionParseTime));
        System.setProperty("jsonapi.baseline.serialization", String.valueOf(serializationTime));
    }
}