package com.github.jasminb.jsonapi;

/**
 * Resource identifier handler.
 *
 * <p>
 *     Provides users with ability to use custom types as resource identifier objects.
 * </p>
 *
 * @author jbegic
 */
public interface ResourceIdHandler {
	
	/**
	 * Convert identifier to {@link String}.
	 *
	 * @param identifier to convert
	 * @return {@link String} identifier string representation
	 */
	String asString(Object identifier);
	
	/**
	 * Create identifier object by consuming its string representation.
	 *
	 * @param source {@link String} identifier
	 * @return target object
	 */
	Object fromString(String source);
}
