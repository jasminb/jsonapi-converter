package com.github.jasminb.jsonapi;

import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration that defines list of deserialization features that can be set to {@link ResourceConverter}.
 *
 * @author jbegic
 */
public enum DeserializationFeature {

	/**
	 * This option enforces presence of the 'id' attribute in resources being parsed.
	 */
	REQUIRE_RESOURCE_ID(true);

	final boolean enabled;

	DeserializationFeature(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns set of features that are enabled by default.
	 * @return returns features that are enabled by default
	 */
	public static Set<DeserializationFeature> getDefaultFeatures() {
		Set<DeserializationFeature> result = new HashSet<>();

		for (DeserializationFeature deserializationFeature : values()) {
			if (deserializationFeature.enabled) {
				result.add(deserializationFeature);
			}
		}

		return result;
	}
}
