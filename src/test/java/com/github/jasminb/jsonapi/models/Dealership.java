package com.github.jasminb.jsonapi.models;

import java.util.Collection;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;

@Type("dealerships")
public class Dealership {
	@Id
	private String id;
	private String name;
	private String city;

	@Relationship("inventory")
	private Collection<Driveable> automobiles;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public Collection<Driveable> getAutomobiles() {
		return automobiles;
	}

	public void setAutomobiles(Collection<Driveable> automobiles) {
		this.automobiles = automobiles;
	}
}
