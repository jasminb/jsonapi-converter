package com.github.jasminb.jsonapi.discovery;

import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating JsonProcessor instances with automatic service discovery.
 *
 * Golden Path Phase 3 - Code Generation (Diff 3)
 * Epic 2: Service Discovery Framework
 */
public class JsonProcessorFactory {

    private static final Map<String, JsonProcessor> cache = new ConcurrentHashMap<>();
    private static final Map<String, JsonProcessorProvider> providers = new HashMap<>();
    private static JsonProcessor defaultProcessor;
    private static List<JsonProcessorProvider> availableProviders;

    static {
        // Initialize available providers
        discoverProviders();
    }

    /**
     * Create a default JsonProcessor using the highest priority available implementation.
     */
    public static synchronized JsonProcessor createDefault() {
        if (defaultProcessor == null) {
            List<JsonProcessorProvider> sortedProviders = getSortedAvailableProviders();
            if (sortedProviders.isEmpty()) {
                throw new RuntimeException("No JSON processor implementations found on classpath. " +
                    "Please ensure Jackson, Gson, or JSON-B is available.");
            }

            JsonProcessorProvider selected = sortedProviders.get(0);
            defaultProcessor = selected.create();

            // Cache by provider name as well
            cache.put(selected.getName(), defaultProcessor);
        }
        return defaultProcessor;
    }

    /**
     * Create a specific JsonProcessor by name (e.g., "jackson", "gson").
     */
    public static JsonProcessor create(String name) {
        JsonProcessor cached = cache.get(name);
        if (cached != null) {
            return cached;
        }

        JsonProcessorProvider provider = providers.get(name.toLowerCase());
        if (provider == null) {
            throw new RuntimeException("Unknown JSON processor: " + name +
                ". Available processors: " + getAvailableProcessors());
        }

        if (!provider.isAvailable()) {
            throw new RuntimeException("JSON processor '" + name +
                "' is not available. Required dependencies may be missing from classpath.");
        }

        JsonProcessor processor = provider.create();
        cache.put(name, processor);
        return processor;
    }

    /**
     * Create a JsonProcessor with specific configuration.
     * Configured processors are not cached.
     */
    public static JsonProcessor create(String name, JsonProcessorConfig config) {
        JsonProcessorProvider provider = providers.get(name.toLowerCase());
        if (provider == null) {
            throw new RuntimeException("Unknown JSON processor: " + name);
        }

        if (!provider.isAvailable()) {
            throw new RuntimeException("JSON processor '" + name + "' is not available");
        }

        return provider.create(config);
    }

    /**
     * Get list of available processor names.
     */
    public static List<String> getAvailableProcessors() {
        List<JsonProcessorProvider> sortedProviders = getSortedAvailableProviders();
        List<String> names = new ArrayList<String>();
        for (JsonProcessorProvider provider : sortedProviders) {
            names.add(provider.getName());
        }
        return names;
    }

    /**
     * Get detailed diagnostics about available processors.
     */
    public static JsonProcessorDiagnostics getDiagnostics() {
        return new JsonProcessorDiagnostics();
    }

    /**
     * Clear all cached processor instances.
     */
    public static synchronized void clearCache() {
        cache.clear();
        defaultProcessor = null;
    }

    /**
     * Set available providers (mainly for testing).
     */
    public static synchronized void setAvailableProviders(List<JsonProcessorProvider> testProviders) {
        availableProviders = new ArrayList<>(testProviders);
        providers.clear();
        for (JsonProcessorProvider provider : testProviders) {
            providers.put(provider.getName().toLowerCase(), provider);
        }
        clearCache();
    }

    /**
     * Reset to original discovered providers (mainly for testing).
     */
    public static synchronized void resetDiscovery() {
        providers.clear();
        availableProviders = null;
        discoverProviders();
        clearCache();
    }

    // ===== PRIVATE METHODS =====

    private static void discoverProviders() {
        availableProviders = new ArrayList<>();

        // Try to discover Jackson
        try {
            Class<?> jacksonProviderClass = Class.forName(
                "com.github.jasminb.jsonapi.jackson.JacksonJsonProcessorProvider");
            JsonProcessorProvider jacksonProvider =
                (JsonProcessorProvider) jacksonProviderClass.getDeclaredConstructor().newInstance();
            availableProviders.add(jacksonProvider);
            providers.put("jackson", jacksonProvider);
        } catch (Exception e) {
            // Jackson not available, continue with other providers
        }

        // TODO: Add discovery for Gson and JSON-B providers when implemented
    }

    private static List<JsonProcessorProvider> getSortedAvailableProviders() {
        List<JsonProcessorProvider> available = new ArrayList<JsonProcessorProvider>();

        // Filter available providers
        for (JsonProcessorProvider provider : availableProviders) {
            if (provider.isAvailable()) {
                available.add(provider);
            }
        }

        // Sort by priority (highest first)
        Collections.sort(available, new Comparator<JsonProcessorProvider>() {
            @Override
            public int compare(JsonProcessorProvider a, JsonProcessorProvider b) {
                return Integer.compare(b.getPriority(), a.getPriority());
            }
        });

        return available;
    }
}