package com.github.jasminb.jsonapi.exceptions;

/**
 * RepeatedRelationshipsException
 * Thrown when a @Relationship is defined more than once with the same name within a class.
 *
 * @author dulleh.
 */
public class RepeatedRelationshipsException extends RuntimeException {

    private final String relationshipName;

    /**
     * Constructor.
     *
     * @param relationshipName The relationship name that is registered more than once.
     */
    public RepeatedRelationshipsException(String relationshipName) {
        super("@Relationship(" + relationshipName + ") set on multiple fields. " +
                "If the json returned for this relationship can be of multiple types (polymorphic), " +
                "please use @PolymorphicRelationship.");
        this.relationshipName = relationshipName;
    }

    /**
     * Returns the repeated relationship name which caused the exception.
     *
     * @return
     */
    public String getType() {
        return relationshipName;
    }
}
