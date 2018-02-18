package com.github.jasminb.jsonapi;

/**
 * Handles {@link Long} as resource identifier type.
 *
 * @author jbegic
 */
public class LongIdHandler implements ResourceIdHandler {
	
	/**
	 * Creates new LongIdHandler.
	 *
	 */
	public LongIdHandler() {
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
	public Long fromString(String source) {
		if (source != null) {
			return Long.valueOf(source);
		}
		return null;
	}
}