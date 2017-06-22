package com.github.jasminb.jsonapi.exceptions;

/**
 * Created by pperera on 23/6/17.
 */
public class UnregisteredClassForType extends RuntimeException {

    private final String type;

    public UnregisteredClassForType(String type) {
        super("No class was registered for type '" + type + "'.");
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
