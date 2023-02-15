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
	private List<String> relationshipExludes;
	private Boolean serializeMeta;
	private Boolean serializeLinks;
	private Boolean serializeId;

	private SerializationSettings() {
		// Hide CTOR
	}

	/**
	 * Checks if relationship with provided name has been explicitly marked for inclusion in serialized object.
	 * @param relationshipName {@link String} relationship name
	 * @return {@link Boolean}
	 */
	public boolean isRelationshipIncluded(String relationshipName) {
		return relationshipIncludes.contains(relationshipName);
	}

	/**
	 * Checks if relationship with provided name has been explicitly marked for exclusion in serialized object.
	 * @param relationshipName {@link String} relationship name
	 * @return {@link Boolean}
	 */
	public boolean isRelationshipExcluded(String relationshipName) {
		return relationshipExludes.contains(relationshipName);
	}

	/**
	 * Returns <code>true</code> in case there is at least one relationship that should be included during serialization.
	 *
	 * @return {@link Boolean}
	 */
	public boolean hasIncludedRelationships() {
		Set<String> includedRelationships = new HashSet<>(relationshipIncludes);
		includedRelationships.removeAll(relationshipExludes);

		return !includedRelationships.isEmpty();
	}

	/**
	 * Returns meta serialization flag.
	 * @return {@link Boolean}
	 */
	public Boolean serializeMeta() {
		return serializeMeta;
	}

	/**
	 * Returns links serialization flag.
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
	 * Serialisation settings builder.
	 */
	public static class Builder {
		private List<String> relationshipIncludes = new ArrayList<>();
		private List<String> relationshipExludes = new ArrayList<>();
		private Boolean serializeMeta;
		private Boolean serializeLinks;
		private Boolean serializeId;

		/**
		 * Explicitly enable relationship serialisation.
		 * @param relationshipName {@link String} relationship name
		 * @return {@link Builder}
		 */
		public Builder includeRelationship(String relationshipName) {
			relationshipIncludes.add(relationshipName);
			return this;
		}

		/**
		 * Explicitly disable relationship serialisation.
		 * @param relationshipName {@link String} relationship name
		 * @return {@link Builder}
		 */
		public Builder excludedRelationships(String relationshipName) {
			relationshipExludes.add(relationshipName);
			return this;
		}

		/**
		 * Enable or disable meta serialization.
		 * @param flag {@link Boolean} serialization flag
		 * @return {@link Builder}
		 */
		public Builder serializeMeta(Boolean flag) {
			serializeMeta = flag;
			return this;
		}

		/**
		 * Enable or disable links serialization.
		 * @param flag {@link Boolean} serialization flag
		 * @return {@link Builder}
		 */
		public Builder serializeLinks(Boolean flag) {
			serializeLinks = flag;
			return this;
		}

		/**
		 * Enable or disable id serialization.
		 * @param flag {@link Boolean} serialization flag
		 * @return {@link Builder}
		 */
		public Builder serializeId(Boolean flag) {
			serializeId = flag;
			return this;
		}

		/**
		 * Create new SerialisationSettings instance.
		 * @return {@link SerializationSettings}
		 */
		public SerializationSettings build() {
			SerializationSettings result = new SerializationSettings();
			result.relationshipIncludes = new ArrayList<>(relationshipIncludes);
			result.relationshipExludes = new ArrayList<>(relationshipExludes);
			result.serializeLinks = serializeLinks;
			result.serializeMeta = serializeMeta;
			result.serializeId = serializeId;
			return result;
		}
	}



}
