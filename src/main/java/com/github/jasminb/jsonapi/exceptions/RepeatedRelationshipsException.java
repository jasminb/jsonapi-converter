package com.github.jasminb.jsonapi.exceptions;

/**
 * RepeatedRelationshipsException
 * Thrown when a @Relationship is defined more than once with the same name within a class.
 *
 * @author dulleh.
 */
public class RepeatedRelationshipsException extends RuntimeException {

    private final String relationshipName;
    private final Class clazz;

    /**
     * Constructor.
     *
     * @param relationshipName The relationship name that is registered more than once.
     * @param clazz The class being parsed when the error was thrown.
     */
    public RepeatedRelationshipsException(String relationshipName, Class clazz) {
        super("@Relationship(" + relationshipName + ") set on multiple fields in " + clazz + ". " +
                "If the json returned for this relationship can be of multiple types (polymorphic), " +
                "please use @PolymorphicRelationship.");
        this.relationshipName = relationshipName;
        this.clazz = clazz;
    }

    /**
     * Returns the repeated relationship name which caused the exception.
     *
     * @return
     */
    public String getType() {
        return relationshipName;
    }

    /**
     * Returns the class which caused the exception.
     *
     * @return
     */
    public String getClazz() {
        return relationshipName;
    }

}
