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
}
