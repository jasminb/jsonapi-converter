package com.github.jasminb.jsonapi.models.inheritance;

import com.github.jasminb.jsonapi.annotations.Meta;
import com.github.jasminb.jsonapi.annotations.Relationship;

/**
 * Person class, used for testing.
 *
 * @author jbegic
 */
public class Person extends BaseModel {
	private String firstName;
	private String lastName;

	@Meta
	private PersonMeta meta;

	@Relationship("city")
	private City city;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public PersonMeta getMeta() {
		return meta;
	}

	public void setMeta(PersonMeta meta) {
		this.meta = meta;
	}

	public City getCity() {
		return city;
	}

	public void setCity(City city) {
		this.city = city;
	}

	public static class PersonMeta {
		private String note;

		public String getNote() {
			return note;
		}

		public void setNote(String note) {
			this.note = note;
		}
	}
}
