package com.github.jasminb.jsonapi;


/**
 * Handles {@link String} as resource identifier type.
 *
 * @author jbegic
 */
public class StringIdHandler implements ResourceIdHandler {
	
	/**
	 * Creates new StringIdHandler.
	 *
	 */
	public StringIdHandler() {
		// Default constructor
	}
	
	@Override
	public String asString(Object identifier) {
		if (identifier != null) {
			return String.valueOf(identifier);
		}
		return null;
	}
	
	@Override
	public String fromString(String source) {
		return source;
	}
}