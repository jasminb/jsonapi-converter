package com.github.jasminb.jsonapi.models.inheritance;

import com.github.jasminb.jsonapi.annotations.Type;

/**
 * Used for testing sub-types being returned as elements in data array.
 *
 * @author jbegic
 */
@Type("Movie")
public class Movie extends Video {
	private String movieTitle;

	public String getMovieTitle() {
		return movieTitle;
	}

	public void setMovieTitle(String movieTitle) {
		this.movieTitle = movieTitle;
	}
}
