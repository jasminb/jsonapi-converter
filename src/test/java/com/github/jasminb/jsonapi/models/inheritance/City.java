package com.github.jasminb.jsonapi.models.inheritance;

import com.github.jasminb.jsonapi.annotations.Type;

/**
 * City class, used for testing.
 *
 * @author jbegic
 */
@Type("city")
public class City extends BaseModel {

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
