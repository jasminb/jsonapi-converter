package com.github.jasminb.jsonapi.discovery;

import com.github.jasminb.jsonapi.abstraction.JsonProcessor;

/**
 * Provider interface for JSON processor implementations.
 * Each JSON library (Jackson, Gson, JSON-B) should provide an implementation.
 *
 * Golden Path Phase 3 - Code Generation (Diff 3)
 * Epic 2: Service Discovery Framework
 */
public interface JsonProcessorProvider {

    /**
     * Check if this provider is available (i.e., required dependencies are on classpath).
     */
    boolean isAvailable();

    /**
     * Create a JsonProcessor with default configuration.
     */
    JsonProcessor create();

    /**
     * Create a JsonProcessor with custom configuration.
     */
    JsonProcessor create(JsonProcessorConfig config);

    /**
     * Get the priority for automatic selection.
     * Higher priority providers are selected first when multiple are available.
     */
    int getPriority();

    /**
     * Get the name of this provider (e.g., "jackson", "gson", "jsonb").
     */
    String getName();

    /**
     * Get a description of this provider.
     */
    String getDescription();

    /**
     * Get version information if available.
     */
    String getVersion();
}