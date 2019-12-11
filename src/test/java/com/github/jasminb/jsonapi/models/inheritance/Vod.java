package com.github.jasminb.jsonapi.models.inheritance;

import com.github.jasminb.jsonapi.annotations.Type;

/**
 * Used for testing sub-types being returned as elements in data array.
 *
 * @author jbegic
 */
@Type("Vod")
public class Vod extends Video {

	private String vodTitle;

	public String getVodTitle() {
		return vodTitle;
	}

	public void setVodTitle(String vodTitle) {
		this.vodTitle = vodTitle;
	}
}
