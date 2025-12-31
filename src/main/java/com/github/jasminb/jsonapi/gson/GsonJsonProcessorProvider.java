package com.github.jasminb.jsonapi.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.discovery.JsonProcessorProvider;
import com.github.jasminb.jsonapi.discovery.JsonProcessorConfig;
import com.github.jasminb.jsonapi.discovery.FieldNamingStrategy;
import com.github.jasminb.jsonapi.discovery.SerializationInclusion;

/**
 * Gson implementation of JsonProcessorProvider.
 *
 * Epic 4: Alternative JSON Library Implementations - Gson Support
 */
public class GsonJsonProcessorProvider implements JsonProcessorProvider {

    private static final int GSON_PRIORITY = 90; // Slightly lower than Jackson

    @Override
    public boolean isAvailable() {
        try {
            // Check if Gson classes are available
            Class.forName("com.google.gson.Gson");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public JsonProcessor create() {
        Gson gson = new GsonBuilder()
            .serializeNulls() // Include null values by default to match Jackson behavior
            .create();
        return new GsonJsonProcessor(gson);
    }

    @Override
    public JsonProcessor create(JsonProcessorConfig config) {
        GsonBuilder builder = new GsonBuilder();

        // Apply field naming strategy
        FieldNamingPolicy namingPolicy = convertNamingStrategy(config.getFieldNamingStrategy());
        if (namingPolicy != null) {
            builder.setFieldNamingPolicy(namingPolicy);
        }

        // Apply serialization inclusion - Gson handles this differently
        // NON_NULL is default behavior, for others we need custom logic
        SerializationInclusion inclusion = config.getSerializationInclusion();
        if (inclusion == SerializationInclusion.NON_NULL) {
            // Default Gson behavior - don't serialize nulls
        } else if (inclusion == SerializationInclusion.ALWAYS) {
            builder.serializeNulls();
        }
        // NON_EMPTY and NON_DEFAULT would require custom serializers in Gson

        // Other configurations
        if (config.isAllowComments()) {
            // Gson doesn't support comments in JSON, but we can ignore this setting
        }

        // Gson is generally more lenient than Jackson, so failOnUnknownProperties
        // doesn't have a direct equivalent, but that's okay for compatibility

        return new GsonJsonProcessor(builder.create());
    }

    @Override
    public int getPriority() {
        return GSON_PRIORITY;
    }

    @Override
    public String getName() {
        return "gson";
    }

    @Override
    public String getDescription() {
        return "Google Gson JSON processor - Simple and lightweight JSON library";
    }

    @Override
    public String getVersion() {
        try {
            // Gson doesn't provide version info in the same way as Jackson
            Package pkg = Gson.class.getPackage();
            return pkg != null ? pkg.getImplementationVersion() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private FieldNamingPolicy convertNamingStrategy(FieldNamingStrategy strategy) {
        if (strategy == null) return null;

        switch (strategy) {
            case SNAKE_CASE:
                return FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
            case KEBAB_CASE:
                return FieldNamingPolicy.LOWER_CASE_WITH_DASHES;
            case UPPER_CAMEL_CASE:
                return FieldNamingPolicy.UPPER_CAMEL_CASE;
            case CAMEL_CASE:
            default:
                return null; // Default Gson behavior
        }
    }
}