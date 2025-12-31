package com.github.jasminb.jsonapi.discovery;

/**
 * Configuration for JsonProcessor creation.
 * Provides common configuration options that can be applied across different JSON libraries.
 *
 * Golden Path Phase 3 - Code Generation (Diff 3)
 * Epic 2: Service Discovery Framework
 */
public class JsonProcessorConfig {

    private FieldNamingStrategy fieldNamingStrategy = FieldNamingStrategy.CAMEL_CASE;
    private SerializationInclusion serializationInclusion = SerializationInclusion.ALWAYS;
    private boolean failOnUnknownProperties = false;
    private boolean allowComments = false;

    private JsonProcessorConfig() {}

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public FieldNamingStrategy getFieldNamingStrategy() {
        return fieldNamingStrategy;
    }

    public SerializationInclusion getSerializationInclusion() {
        return serializationInclusion;
    }

    public boolean isFailOnUnknownProperties() {
        return failOnUnknownProperties;
    }

    public boolean isAllowComments() {
        return allowComments;
    }

    /**
     * Builder for JsonProcessorConfig.
     */
    public static class Builder {
        private final JsonProcessorConfig config = new JsonProcessorConfig();

        public Builder fieldNamingStrategy(FieldNamingStrategy strategy) {
            config.fieldNamingStrategy = strategy;
            return this;
        }

        public Builder serializationInclusion(SerializationInclusion inclusion) {
            config.serializationInclusion = inclusion;
            return this;
        }

        public Builder failOnUnknownProperties(boolean fail) {
            config.failOnUnknownProperties = fail;
            return this;
        }

        public Builder allowComments(boolean allow) {
            config.allowComments = allow;
            return this;
        }

        public JsonProcessorConfig build() {
            return config;
        }
    }
}