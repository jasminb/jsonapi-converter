package com.github.jasminb.jsonapi.exceptions;

/**
 * InvalidJsonApiResourceException implementation. <br/>
 * This exception is thrown when the JSON provided is not valid JSON-API.
 * I.e. The document must contain at least one of 'data', 'error' or 'meta' nodes.
 *
 * @author prawana-perera.
 */
public class InvalidJsonApiResourceException extends RuntimeException {

    /**
     * Creates a new InvalidJsonApiResourceException.
     */
    public InvalidJsonApiResourceException() {
        super("Resource must contain at least one of 'data', 'error' or 'meta' nodes.");
    }

    /**
     * Creates a new InvalidJsonApiResourceException.
     *
     * @param errorMessage detail message containing spec for resource that was invalid.
     */
    public InvalidJsonApiResourceException(String errorMessage) {
        super(errorMessage);
    }
}
