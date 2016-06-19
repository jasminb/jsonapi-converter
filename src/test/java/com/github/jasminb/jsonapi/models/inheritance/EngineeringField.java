package com.github.jasminb.jsonapi.models.inheritance;

import com.github.jasminb.jsonapi.annotations.Type;

/**
 * EngineeringField class, used for testing
 *
 * @author jbegic
 */
@Type("engineering_field")
public class EngineeringField extends BaseModel {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
