package com.github.jasminb.jsonapi.resolutionstrategy;


import com.github.jasminb.jsonapi.RelType;
import com.github.jasminb.jsonapi.ResolutionStrategy;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;

import java.util.List;

@Type("articles")
public class Article {
	@Id
	private String id;

	private String title;

	@Relationship(value = "author", resolve = true, relType = RelType.RELATED, strategy = ResolutionStrategy.REF)
	private String author;

	@Relationship(value = "comments", resolve = true, relType = RelType.RELATED, strategy = ResolutionStrategy.OBJECT)
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

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public List<Comment> getComments() {
		return comments;
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}
}
