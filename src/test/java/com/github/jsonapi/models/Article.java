package com.github.jsonapi.models;


import com.github.jsonapi.annotations.Id;
import com.github.jsonapi.annotations.Relationship;
import com.github.jsonapi.annotations.Type;

import java.util.List;

@Type("articles")
public class Article {
	@Id
	private String id;

	private String title;

	@Relationship("author")
	private Author author;

	@Relationship("comments")
	private List<Comment> comments;


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Author getAuthor() {
		return author;
	}

	public void setAuthor(Author author) {
		this.author = author;
	}

	public List<Comment> getComments() {
		return comments;
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}
}
