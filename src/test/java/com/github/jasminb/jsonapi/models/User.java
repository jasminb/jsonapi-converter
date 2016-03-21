package com.github.jasminb.jsonapi.models;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Meta;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;

import java.util.List;

@Type("users")
public class User {

	public static class UserMeta {
		public String token;

		public String getToken() {
			return token;
		}
	}

	@Id
	public String id;
	public String name;

	@Relationship("statuses")
	private List<Status> statuses;

	@Meta
	public UserMeta meta;

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

	public UserMeta getMeta() {
		return meta;
	}

}