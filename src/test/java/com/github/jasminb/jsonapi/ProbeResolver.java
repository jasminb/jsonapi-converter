package com.github.jasminb.jsonapi;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple global RelationshipResolver implementation that maintains a count of responses for each resolution of a
 * relationship url.
 */
public class ProbeResolver implements RelationshipResolver {

    /**
     * Map of relationship urls to the response JSON
     */
    private Map<String, String> responseMap;

    /**
     * Map of relationship to a count of the times they have been resolved
     */
    private Map<String, Integer> resolved = new HashMap<>();

	/**
	 * Construct a new instance, supplying a Map of relationship URLs to a String of serialized JSON.
	 *
	 * @param responseMap response JSON keyed by relationship url
     */
    public ProbeResolver(Map<String, String> responseMap) {
        this.responseMap = responseMap;
    }

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the supplied {@code relationshipURL} is missing from the response Map, then an
	 * {@code IllegalArgumentException} is thrown.
	 * </p>
	 * @param relationshipURL URL. eg. <code>users/1</code> or <code>https://api.myhost.com/uers/1</code>
	 * @return
	 * @throws IllegalArgumentException if {@code relationshipURL} is missing from the response Map.
     */
    @Override
    public byte[] resolve(String relationshipURL) {
        if (responseMap.containsKey(relationshipURL)) {
            if (resolved.containsKey(relationshipURL)) {
                int count = resolved.get(relationshipURL);
                resolved.put(relationshipURL, ++count);
            } else {
                resolved.put(relationshipURL, 1);
            }
            return responseMap.get(relationshipURL).getBytes();
        }
        throw new IllegalArgumentException("Unable to resolve '" + relationshipURL + "', missing response map " +
                "entry.");
    }

	/**
	 * Returns a map of relationship URLs and the number of times each URL was resolved.
	 *
	 * @return the resolution map
     */
    public Map<String, Integer> getResolved() {
        return resolved;
    }
}
