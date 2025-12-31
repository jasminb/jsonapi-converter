package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.abstraction.JsonElement;
import com.github.jasminb.jsonapi.abstraction.JsonProcessor;
import com.github.jasminb.jsonapi.models.errors.Errors;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.ResponseBody;

/**
 * Utility class providing methods needed for parsing JSON API Spec errors.
 *
 * @author jbegic
 */
public class ErrorUtils {

    private ErrorUtils() {
        // Private constructor
    }

    /**
     * Parses provided ResponseBody and returns it as T.
     *
     * @param mapper        Jackson Object mapper instance
     * @param errorResponse error response body
     * @return T collection
     * @throws IOException
     * @deprecated Use {@link #parseErrorResponse(JsonProcessor, byte[], Class)} instead
     */
    @Deprecated
    public static <T extends Errors> T parseErrorResponse(ObjectMapper mapper, ResponseBody errorResponse, Class<T> cls) throws IOException {
        return mapper.readValue(errorResponse.bytes(), cls);
    }

    /**
     * Parses provided byte array and returns it as T.
     *
     * @param processor     JsonProcessor instance
     * @param errorResponse error response bytes
     * @param cls           target class
     * @return T collection
     */
    public static <T extends Errors> T parseErrorResponse(JsonProcessor processor, byte[] errorResponse, Class<T> cls) {
        return processor.readValue(errorResponse, cls);
    }

    /**
     * Parses provided JsonNode and returns it as T.
     *
     * @param mapper        Jackson Object mapper instance
     * @param errorResponse error response body
     * @return T collection
     * @throws JsonProcessingException thrown in case JsonNode cannot be parsed
     * @deprecated Use {@link #parseError(JsonElement, Class)} instead
     */
    @Deprecated
    public static <T extends Errors> T parseError(ObjectMapper mapper, JsonNode errorResponse, Class<T> cls) throws JsonProcessingException {
        return mapper.treeToValue(errorResponse, cls);
    }

    /**
     * Parses provided JsonElement and returns it as T using global JsonProcessor.
     * Uses the default JsonProcessor from discovery if not in a ResourceConverter context.
     *
     * @param errorResponse error response element
     * @param cls           target class
     * @return T collection
     */
    public static <T extends Errors> T parseError(JsonElement errorResponse, Class<T> cls) {
        // Get the processor from thread-local context, or create a default one
        JsonProcessor processor = ErrorParseContext.getProcessor();
        return processor.treeToValue(errorResponse, cls);
    }

    /**
     * Parses provided JsonElement and returns it as T.
     *
     * @param processor     JsonProcessor instance
     * @param errorResponse error response element
     * @param cls           target class
     * @return T collection
     */
    public static <T extends Errors> T parseError(JsonProcessor processor, JsonElement errorResponse, Class<T> cls) {
        return processor.treeToValue(errorResponse, cls);
    }

    /**
     * @deprecated Use {@link #parseError(JsonProcessor, InputStream, Class)} instead
     */
    @Deprecated
    public static <T extends Errors> T parseError(ObjectMapper mapper, InputStream errorResponse, Class<T> cls) throws IOException {
        return mapper.readValue(errorResponse, cls);
    }

    /**
     * Parses provided InputStream and returns it as T.
     *
     * @param processor     JsonProcessor instance
     * @param errorResponse error response stream
     * @param cls           target class
     * @return T collection
     */
    public static <T extends Errors> T parseError(JsonProcessor processor, InputStream errorResponse, Class<T> cls) {
        return processor.readValue(errorResponse, cls);
    }

    /**
     * Thread-local context for error parsing within ResourceConverter.
     * This allows ValidationUtils to access the JsonProcessor without changing its signature.
     */
    public static class ErrorParseContext {
        private static final ThreadLocal<JsonProcessor> threadLocalProcessor = new ThreadLocal<>();

        public static void setProcessor(JsonProcessor processor) {
            threadLocalProcessor.set(processor);
        }

        public static JsonProcessor getProcessor() {
            JsonProcessor processor = threadLocalProcessor.get();
            if (processor == null) {
                // Fallback to default processor
                processor = com.github.jasminb.jsonapi.discovery.JsonProcessorFactory.createDefault();
            }
            return processor;
        }

        public static void clear() {
            threadLocalProcessor.remove();
        }
    }
}
