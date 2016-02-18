package com.github.jsonapi;

import com.github.jsonapi.annotations.Id;
import com.github.jsonapi.annotations.Relationship;
import com.github.jsonapi.annotations.Type;

@Type(name = "statuses")
public class Status {
	@Id
	private String id;
	private String content;
	private Integer commentCount;
	private Integer likeCount;

	@Relationship(name = "user")
	private User user;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Integer getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(Integer commentCount) {
		this.commentCount = commentCount;
	}

	public Integer getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(Integer likeCount) {
		this.likeCount = likeCount;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public String toString() {
		return "Status{" +
				"id='" + id + '\'' +
				", content='" + content + '\'' +
				", commentCount=" + commentCount +
				", likeCount=" + likeCount +
				", user=" + user +
				'}';
	}
}
