package com.github.jasminb.jsonapi.exceptions;

/**
 * RepeatedPolymorphRelationshipsException
 * Thrown when a @PolymorphRelationship is defined more than once with the same type within a class.
 *
 * @author dulleh.
 */
public class RepeatedPolymorphRelationshipsException extends RuntimeException {

    private final String relationshipName;
    private final String targetType;
    private final Class clazz;

    /**
     * Constructor.
     *
     * @param relationshipName The relationship name of the relationship
     * @param targetType The type for which the relationship is repeated.
     * @param clazz The class which was being parsed when the error was thrown.
     */
    public RepeatedPolymorphRelationshipsException(String relationshipName, String targetType, Class clazz) {
        super("@PolymorphRelationship(" + relationshipName + ") set on multiple fields of the type " + targetType +
                " in " + clazz  + ". Fields annotated with @PolymorphRelationship must have unique types defined.");
        this.relationshipName = relationshipName;
        this.targetType = targetType;
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
     * Returns the repeated type which caused the exception.
     *
     * @return
     */
    public String getTargetType() {
        return targetType;
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
