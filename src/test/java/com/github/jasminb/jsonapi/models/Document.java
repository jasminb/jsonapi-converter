package com.github.jasminb.jsonapi.models;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;

import java.util.UUID;

@Type("documents")
public class Document {
	@Id
	private String id;

	@Relationship("relatedDocument")
	private Document relatedDocument;

	private String title;

	public Document() {
	}

	public Document(UUID id, String title, Document relatedDocument) {
		this.id = id.toString();
		this.title = title;
		this.relatedDocument = relatedDocument;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public Document getRelatedDocument() {
		return relatedDocument;
	}
}
