package com.github.jasminb.jsonapi.exceptions;

/**
 * InvalidMetaDataTypeException implementation. <br/>
 * This exception is thrown when a <a href="http://jsonapi.org/format/#document-meta">JSON-API meta object</a>
 * that is provided is not a {@code Map}. It may be an array instead of a type that can be de-serialized
 * to a {@code Map}.
 *
 * @author ianrumac.
 */

public class InvalidMetaDataTypeException extends RuntimeException {

    /**
     * Creates a new InvalidMetaDataTypeException.
     */
    public InvalidMetaDataTypeException() {
        super("Failed to parse JSON-API meta object to a Map type.");
    }
}
