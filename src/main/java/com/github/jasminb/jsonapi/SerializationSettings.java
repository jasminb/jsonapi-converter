package com.github.jasminb.jsonapi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Serialization settings.
 *
 * @author jbegic
 */
public class SerializationSettings {
	private List<String> relationshipIncludes;
	private List<String> relationshipExcludes;
	private Boolean serializeMeta;
	private Boolean serializeLinks;
	private Boolean serializeId;
	private Boolean serializeLocalId;
	private Boolean serializeJSONAPIObject;

	private SerializationSettings() {
		// Hide CTOR
	}

	/**
	 * Checks if relationship with provided name has been explicitly marked for inclusion in serialized object.
	 *
	 * @param relationshipName {@link String} relationship name
	 * @return {@link Boolean}
	 */
	public boolean isRelationshipIncluded(String relationshipName) {
		return relationshipIncludes.contains(relationshipName);
	}

	/**
	 * Checks if relationship with provided name has been explicitly marked for exclusion in serialized object.
	 *
	 * @param relationshipName {@link String} relationship name
	 * @return {@link Boolean}
	 */
	public boolean isRelationshipExcluded(String relationshipName) {
		return relationshipExcludes.contains(relationshipName);
	}

	/**
	 * Returns <code>true</code> in case there is at least one relationship that should be included during serialization.
	 *
	 * @return {@link Boolean}
	 */
	public boolean hasIncludedRelationships() {
		Set<String> includedRelationships = new HashSet<>(relationshipIncludes);
		includedRelationships.removeAll(relationshipExcludes);

		return !includedRelationships.isEmpty();
	}

	/**
	 * Returns meta serialization flag.
	 *
	 * @return {@link Boolean}
	 */
	public Boolean serializeMeta() {
		return serializeMeta;
	}

	/**
	 * Returns links serialization flag.
	 *
	 * @return {@link Boolean}
	 */
	public Boolean serializeLinks() {
		return serializeLinks;
	}

	/**
	 * Returns {@link com.github.jasminb.jsonapi.annotations.Id} serialization flag.
	 *
	 * @return {@link Boolean}
	 */
	public Boolean serializeId() {
		return serializeId;
	}

	/**
	 * Returns {@link com.github.jasminb.jsonapi.annotations.LocalId} serialization flag.
	 *
	 * @return {@link Boolean}
	 */
	public Boolean serializeLocalId() {
		return serializeLocalId;
	}

	/**
	 * Returns JSON API object serialization flag.
	 *
	 * @return {@link Boolean}
	 */
	public Boolean serializeJSONAPIObject() {
		return serializeJSONAPIObject;
	}

	/**
	 * Serialisation settings builder.
	 */
	public static class Builder {
		private final List<String> relationshipIncludes = new ArrayList<>();
		private final List<String> relationshipExcludes = new ArrayList<>();
		private Boolean serializeMeta;
		private Boolean serializeLinks;
		private Boolean serializeId;
		private Boolean serializeLocalId;
		private Boolean serializeJSONAPIObject;

		/**
		 * Explicitly enable relationship serialisation.
		 *
		 * @param relationshipName {@link String} relationship name
		 * @return {@link Builder}
		 */
		public Builder includeRelationship(String relationshipName) {
			relationshipIncludes.add(relationshipName);
			return this;
		}

		/**
		 * Explicitly disable relationship serialisation.
		 *
		 * @param relationshipName {@link String} relationship name
		 * @return {@link Builder}
		 */
		public Builder excludedRelationships(String relationshipName) {
			relationshipExcludes.add(relationshipName);
			return this;
		}

		/**
		 * Enable or disable meta serialization.
		 *
		 * @param flag {@link Boolean} serialization flag
		 * @return {@link Builder}
		 */
		public Builder serializeMeta(Boolean flag) {
			serializeMeta = flag;
			return this;
		}

		/**
		 * Enable or disable links serialization.
		 *
		 * @param flag {@link Boolean} serialization flag
		 * @return {@link Builder}
		 */
		public Builder serializeLinks(Boolean flag) {
			serializeLinks = flag;
			return this;
		}

		/**
		 * Enable or disable id serialization.
		 *
		 * @param flag {@link Boolean} serialization flag
		 * @return {@link Builder}
		 */
		public Builder serializeId(Boolean flag) {
			serializeId = flag;
			return this;
		}

		/**
		 * Enable or disable local id serialization.
		 *
		 * @param flag {@link Boolean} serialization flag
		 * @return {@link Builder}
		 */
		public Builder serializeLocalId(Boolean flag) {
			serializeLocalId = flag;
			return this;
		}

		/**
		 * Enable or disable JSON API object serialization.
		 *
		 * @param flag {@link Boolean} serialization flag
		 * @return {@link Builder}
		 */
		public Builder serializeJSONAPIObject(Boolean flag) {
			serializeJSONAPIObject = flag;
			return this;
		}

		/**
		 * Create new SerialisationSettings instance.
		 *
		 * @return {@link SerializationSettings}
		 */
		public SerializationSettings build() {
			SerializationSettings result = new SerializationSettings();
			result.relationshipIncludes = new ArrayList<>(relationshipIncludes);
			result.relationshipExcludes = new ArrayList<>(relationshipExcludes);
			result.serializeLinks = serializeLinks;
			result.serializeMeta = serializeMeta;
			result.serializeId = serializeId;
			result.serializeJSONAPIObject = serializeJSONAPIObject;
			result.serializeLocalId = serializeLocalId;
			return result;
		}
	}
}
