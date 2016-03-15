package com.github.jasminb.jsonapi.models.errors;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * JSON API Error model.
 *
 * @author jbegic
 */
public class Error {
	private String id;
	private Links links;
	private String status;
	private String code;
	private String title;
	private String detail;
	private Source source;
	private JsonNode meta;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Links getLinks() {
		return links;
	}

	public void setLinks(Links links) {
		this.links = links;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public JsonNode getMeta() {
		return meta;
	}

	public void setMeta(JsonNode meta) {
		this.meta = meta;
	}

	@Override
	public String toString() {
		return "Error{" +
				"id='" + id + '\'' +
				", links=" + links +
				", status='" + status + '\'' +
				", code='" + code + '\'' +
				", title='" + title + '\'' +
				", detail='" + detail + '\'' +
				", source=" + source +
				", meta=" + meta +
				'}';
	}
}
