package com.github.jasminb.jsonapi;

/**
 * Handles {@link Integer} as resource identifier type.
 *
 * @author jbegic
 */
public class IntegerIdHandler implements ResourceIdHandler {
	
	/**
	 * Creates new IntegerIdHandler.
	 *
	 */
	public IntegerIdHandler() {
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
	public Integer fromString(String source) {
		if (source != null) {
			return Integer.valueOf(source);
		}
		return null;
	}
}