package com.github.jasminb.jsonapi.models.inheritance;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;

/**
 * Engineer class, used for testing.
 *
 * @author jbegic
 */
@Type("engineer")
public class Engineer extends Person {

	@Relationship("field")
	private EngineeringField field;

	public EngineeringField getField() {
		return field;
	}

	public void setField(EngineeringField field) {
		this.field = field;
	}
}
