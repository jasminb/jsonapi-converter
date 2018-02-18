package com.github.jasminb.jsonapi.exceptions;

/**
 * UnregisteredTypeException implementation. <br/>
 * This exception is thrown when a 'type' is not registered with the {@link com.github.jasminb.jsonapi.ResourceConverter}
 * and attempting to de-serialise JSON-API with an unregistered type.
 *
 * @author prawana-perera.
 */
public class UnregisteredTypeException extends RuntimeException {

    private final String type;

    /**
     * Constructor.
     *
     * @param type The type that is not registered
     */
    public UnregisteredTypeException(String type) {
        super("No class was registered for type '" + type + "'.");
        this.type = type;
    }

    /**
     * Returns the unregistered type for which this exception is applicable to.
     *
     * @return
     */
    public String getType() {
        return type;
    }
}
