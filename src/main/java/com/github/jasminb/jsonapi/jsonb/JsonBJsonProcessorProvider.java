package com.github.jasminb.jsonapi.jsonb;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.config.PropertyNamingStrategy;
import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.discovery.JsonProcessorProvider;
import com.github.jasminb.jsonapi.discovery.JsonProcessorConfig;
import com.github.jasminb.jsonapi.discovery.FieldNamingStrategy;
import com.github.jasminb.jsonapi.discovery.SerializationInclusion;

/**
 * JSON-B implementation of JsonProcessorProvider.
 *
 * Epic 4: Alternative JSON Library Implementations - JSON-B Support
 */
public class JsonBJsonProcessorProvider implements JsonProcessorProvider {

    private static final int JSONB_PRIORITY = 80; // Lower than Jackson and Gson

    @Override
    public boolean isAvailable() {
        try {
            // Check if JSON-B classes are available
            Class.forName("jakarta.json.bind.Jsonb");
            Class.forName("jakarta.json.bind.JsonbBuilder");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public JsonProcessor create() {
        Jsonb jsonb = JsonbBuilder.create();
        return new JsonBJsonProcessor(jsonb);
    }

    @Override
    public JsonProcessor create(JsonProcessorConfig config) {
        JsonbConfig jsonbConfig = new JsonbConfig();

        // Apply field naming strategy
        String namingStrategy = convertNamingStrategy(config.getFieldNamingStrategy());
        if (namingStrategy != null) {
            jsonbConfig.withPropertyNamingStrategy(namingStrategy);
        }

        // Apply serialization inclusion
        SerializationInclusion inclusion = config.getSerializationInclusion();
        if (inclusion == SerializationInclusion.NON_NULL) {
            // JSON-B default behavior is to not serialize nulls
        } else if (inclusion == SerializationInclusion.ALWAYS) {
            jsonbConfig.withNullValues(true);
        }
        // NON_EMPTY and NON_DEFAULT are not directly supported in JSON-B

        // Other configurations
        // JSON-B doesn't have direct equivalents for some Jackson/Gson features
        // but provides good defaults

        Jsonb jsonb = JsonbBuilder.create(jsonbConfig);
        return new JsonBJsonProcessor(jsonb);
    }

    @Override
    public int getPriority() {
        return JSONB_PRIORITY;
    }

    @Override
    public String getName() {
        return "json-b";
    }

    @Override
    public String getDescription() {
        return "Jakarta JSON-B processor - Jakarta EE standard for JSON binding";
    }

    @Override
    public String getVersion() {
        try {
            Package pkg = Jsonb.class.getPackage();
            return pkg != null ? pkg.getImplementationVersion() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private String convertNamingStrategy(FieldNamingStrategy strategy) {
        if (strategy == null) return null;

        switch (strategy) {
            case SNAKE_CASE:
                return "LOWER_CASE_WITH_UNDERSCORES";
            case KEBAB_CASE:
                return "LOWER_CASE_WITH_DASHES";
            case UPPER_CAMEL_CASE:
                return "UPPER_CAMEL_CASE";
            case CAMEL_CASE:
            default:
                return "IDENTITY"; // Default JSON-B behavior
        }
    }
}