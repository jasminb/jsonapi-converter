package com.github.jasminb.jsonapi.discovery;

/**
 * Serialization inclusion options.
 *
 * Golden Path Phase 3 - Code Generation (Diff 3)
 * Epic 2: Service Discovery Framework
 */
public enum SerializationInclusion {
    ALWAYS,      // Include all fields
    NON_NULL,    // Exclude null fields
    NON_EMPTY,   // Exclude null and empty fields
    NON_DEFAULT  // Exclude fields with default values
}