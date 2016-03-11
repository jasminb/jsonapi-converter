package com.github.jsonapi.models;

import com.github.jsonapi.annotations.Id;
import com.github.jsonapi.annotations.Relationship;
import com.github.jsonapi.annotations.Type;

import java.util.List;

@Type("users")
public class User {

	@Id
	private String id;
	private String name;

	@Relationship("statuses")
	private List<Status> statuses;


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

	public List<Status> getStatuses() {
		return statuses;
	}

	public void setStatuses(List<Status> statuses) {
		this.statuses = statuses;
	}
}
