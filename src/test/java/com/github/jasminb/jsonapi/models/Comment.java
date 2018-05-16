package com.github.jasminb.jsonapi.models;


import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;

@Type("comments")
public class Comment {
	@Id
	private String id;
	private String body;

	@Relationship("author")
	private Author author;

	@Relationship("about")
	private Commentable about;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Author getAuthor() {
		return author;
	}

	public void setAuthor(Author author) {
		this.author = author;
	}

	public Commentable getAbout() {
		return about;
	}

	public void setAbout(Commentable about) {
		this.about = about;
	}
}
