package com.github.jasminb.jsonapi.models;

import com.github.jasminb.jsonapi.IntegerIdHandler;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;

/**
 * Model class used to test {@link Integer} as resource identifier.
 *
 * @author jbegic
 */
@Type("integer-id-type")
public class IntegerIdResource {
	
	@Id(IntegerIdHandler.class)
	private Integer id;
	
	private String value;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
}
