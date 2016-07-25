package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
     */
    public static <T extends Errors> T parseErrorResponse(ObjectMapper mapper, ResponseBody errorResponse, Class<T> cls) throws IOException {
        return mapper.readValue(errorResponse.bytes(), cls);
    }

    /**
     * Parses provided JsonNode and returns it as T.
     *
     * @param mapper        Jackson Object mapper instance
     * @param errorResponse error response body
     * @return T collection
     * @throws JsonProcessingException thrown in case JsonNode cannot be parsed
     */
    public static <T extends Errors> T parseError(ObjectMapper mapper, JsonNode errorResponse, Class<T> cls) throws JsonProcessingException {
        return mapper.treeToValue(errorResponse, cls);
    }

    public static <T extends Errors> T parseError(ObjectMapper mapper, InputStream errorResponse, Class<T> cls) throws IOException {
        return mapper.readValue(errorResponse, cls);
    }

}
