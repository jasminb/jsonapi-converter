package com.github.jasminb.jsonapi.models;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.github.jasminb.jsonapi.Links;
import com.github.jasminb.jsonapi.RelType;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.RelationshipLinks;
import com.github.jasminb.jsonapi.annotations.Type;

import java.util.Collections;
import java.util.List;

@Type(value = "articles", path = "/articles/{id}")
@JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class, property = "id")
public class Article {
	@Id
	private String id;

	private String title;

	@Relationship(value = "author", resolve = true, relType = RelType.RELATED)
	private Author author;

	@Relationship(value = "comments", resolve = true, path = "relationships/comments", relatedPath = "comments")
	private List<Comment> comments;

    @RelationshipLinks(value = "comments")
    private Links commentRelationshipLinks;

	@Relationship(value = "users", serialiseData = false)
	private List<User> users;

	@RelationshipLinks(value = "users")
	private Links userRelationshipLinks;

	public Article() {
		users = Collections.emptyList();
	}

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

	public List<User> getUsers() {
		return users;
	}

	public void setUsers(List<User> users) {
		this.users = users;
	}

	public Links getUserRelationshipLinks() {
		return userRelationshipLinks;
	}

	public void setUserRelationshipLinks(Links userRelationshipLinks) {
		this.userRelationshipLinks = userRelationshipLinks;
	}

    public Links getCommentRelationshipLinks() {
        return commentRelationshipLinks;
    }

    public void setCommentRelationshipLinks(Links commentRelationshipLinks) {
        this.commentRelationshipLinks = commentRelationshipLinks;
    }
}
