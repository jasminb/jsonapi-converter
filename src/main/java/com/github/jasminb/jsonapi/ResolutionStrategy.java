package com.github.jasminb.jsonapi;

/**
 * Represents different strategies for resolving relationships.
 */
public enum ResolutionStrategy {

    /**
     * Strategy which resolves the relationship URL (as specified by the {@code relType} attribute on the
     * {@code Relationship}) using a {@link RelationshipResolver}, and subsequently deserializes the JSON response into
     * a Java object.
     */
    OBJECT,

    /**
     * Strategy which simply stores the relationship URL (as specified by the {@code relType} attribute on the
     * {@code Relationship}) in String field on an object.
     */
    REF

}
