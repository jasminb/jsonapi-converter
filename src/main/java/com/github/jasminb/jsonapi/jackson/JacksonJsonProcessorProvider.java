package com.github.jasminb.jsonapi.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.discovery.JsonProcessorProvider;
import com.github.jasminb.jsonapi.discovery.JsonProcessorConfig;
import com.github.jasminb.jsonapi.discovery.FieldNamingStrategy;
import com.github.jasminb.jsonapi.discovery.SerializationInclusion;

/**
 * Jackson implementation of JsonProcessorProvider.
 *
 * Golden Path Phase 3 - Code Generation (Diff 3)
 * Epic 3: Jackson Implementation - Provider
 */
public class JacksonJsonProcessorProvider implements JsonProcessorProvider {

    private static final int JACKSON_PRIORITY = 100; // High priority since Jackson is well-established

    @Override
    public boolean isAvailable() {
        try {
            // Check if Jackson classes are available
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public JsonProcessor create() {
        ObjectMapper mapper = new ObjectMapper();
        // Set default serialization inclusion to match ResourceConverter expectations
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return new JacksonJsonProcessor(mapper);
    }

    @Override
    public JsonProcessor create(JsonProcessorConfig config) {
        ObjectMapper mapper = new ObjectMapper();

        // Apply field naming strategy
        PropertyNamingStrategy namingStrategy = convertNamingStrategy(config.getFieldNamingStrategy());
        if (namingStrategy != null) {
            mapper.setPropertyNamingStrategy(namingStrategy);
        }

        // Apply serialization inclusion
        JsonInclude.Include inclusion = convertSerializationInclusion(config.getSerializationInclusion());
        if (inclusion != null) {
            mapper.setSerializationInclusion(inclusion);
        }

        // Apply other configurations
        if (config.isFailOnUnknownProperties()) {
            mapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        }

        if (config.isAllowComments()) {
            mapper.configure(
                com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS, true);
        }

        return new JacksonJsonProcessor(mapper);
    }

    @Override
    public int getPriority() {
        return JACKSON_PRIORITY;
    }

    @Override
    public String getName() {
        return "jackson";
    }

    @Override
    public String getDescription() {
        return "Jackson JSON processor - High performance JSON library";
    }

    @Override
    public String getVersion() {
        try {
            Package pkg = ObjectMapper.class.getPackage();
            return pkg != null ? pkg.getImplementationVersion() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private PropertyNamingStrategy convertNamingStrategy(FieldNamingStrategy strategy) {
        if (strategy == null) return null;

        switch (strategy) {
            case SNAKE_CASE:
                return PropertyNamingStrategy.SNAKE_CASE;
            case KEBAB_CASE:
                return PropertyNamingStrategy.KEBAB_CASE;
            case UPPER_CAMEL_CASE:
                return PropertyNamingStrategy.UPPER_CAMEL_CASE;
            case CAMEL_CASE:
            default:
                return null; // Default Jackson behavior
        }
    }

    private JsonInclude.Include convertSerializationInclusion(SerializationInclusion inclusion) {
        if (inclusion == null) return null;

        switch (inclusion) {
            case NON_NULL:
                return JsonInclude.Include.NON_NULL;
            case NON_EMPTY:
                return JsonInclude.Include.NON_EMPTY;
            case NON_DEFAULT:
                return JsonInclude.Include.NON_DEFAULT;
            case ALWAYS:
            default:
                return JsonInclude.Include.ALWAYS;
        }
    }
}