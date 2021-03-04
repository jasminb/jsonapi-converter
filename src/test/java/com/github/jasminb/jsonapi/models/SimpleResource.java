package com.github.jasminb.jsonapi.models;

import com.github.jasminb.jsonapi.IntegerIdHandler;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;

/**
 * Model class used to test {@link Integer} as resource identifier.
 *
 * @author jbegic
 */
@Type("simple-resource-type")
public class SimpleResource {
	
	@Id(IntegerIdHandler.class)
	private Integer id;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
}
