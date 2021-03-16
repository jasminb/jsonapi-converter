package com.github.jasminb.jsonapi;

import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration that defines list of serialization features that can be set to {@link ResourceConverter}.
 *
 * @author jbegic
 */
public enum SerializationFeature {

	/**
	 * This option if enabled instructs resource converter to serialize entire relationship objects (type and id are
	 * serialized by default)
	 */
	INCLUDE_RELATIONSHIP_ATTRIBUTES(false),

	/**
	 * If enabled, meta attribute will be serialized
	 */
	INCLUDE_META(true),

	/**
	 * If enabled, links attribute will be serialized
	 */
	INCLUDE_LINKS(true),

	/**
	 * If enabled, {@link com.github.jasminb.jsonapi.annotations.Id} attribute will be serialized
	 */
	INCLUDE_ID(true);

	final boolean enabled;

	SerializationFeature(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns set of features that are enabled by default.
	 * @return returns features that are enabled by default
	 */
	public static Set<SerializationFeature> getDefaultFeatures() {
		Set<SerializationFeature> result = new HashSet<>();

		for (SerializationFeature feature : values()) {
			if (feature.enabled) {
				result.add(feature);
			}
		}

		return result;
	}
}
