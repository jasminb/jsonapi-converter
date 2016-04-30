package com.github.jasminb.jsonapi;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the state that must be maintained when recursively resolving relationships for a given {@code Field}.  The
 * implementation that resolves relationships will maintain this state for each {@link Field} being resolved.
 */
class ResolverState {

    /**
     * The relationship type being resolved (e.g. "self", "related")
     */
    private final String relType;

    /**
     * The Java Field for which relationships are being resolved
     */
    private final Field field;

    /**
     * Maintains a set of urls that have been visited when resolving relationships
     */
    private final Set<String> visited = new HashSet<>();

    /**
     * Constructs a new state object.
     *
     * @param field the Java {@code Field} for which relationships are being resolved
     * @param relType the relationship type being navigated (e.g. "self", "related") for this {@code field}
     */
    ResolverState(Field field, String relType) {
        if (relType == null || relType.trim().equals("")) {
            throw new IllegalArgumentException("Missing relationship type.");
        }

        if (field == null) {
            throw new IllegalArgumentException("Cannot manage state for a null Field.");
        }

        this.relType = relType;
        this.field = field;
    }

    /**
     * Used by the relationship resolution implementation to record that a URL has been visited for the purpose of
     * resolving a relationship.  Useful for detecting recursive loops.  Typically the {@code url} will be
     * the value of a "self" or "related" link.  If a link has been visited previously, this method will return
     * {@code true}.
     *
     * @param url a link url
     * @return true if {@code url} has already been visited, {@code false} otherwise
     */
    boolean visited(String url) {
        return !this.visited.add(url);
    }

    /**
     * Used by the relationship resolution implementation to maintain the relationship type being resolved for this
     * {@code {@link #getField() field}.  Typically "related" or "self".
     *
     * @return the relationship type being resolved
     */
    String getRelType() {
        return relType;
    }

    /**
     * Used by the relationship resolution implementation to maintain the {@code Field} being resolved.
     *
     * @return the {@code Field} being resolved
     */
    Field getField() {
        return field;
    }

}
