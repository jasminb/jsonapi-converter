package com.github.jasminb.jsonapi;

import java.util.List;
import java.util.Map;

/**
 * JSON API model.
 *
 * @author jbegic
 */
public class JsonApi {
	private String version;
	private List<String> ext;
	private List<String> profile;
	private Map<String, Object> meta;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<String> getExt() {
		return ext;
	}

	public void setExt(List<String> ext) {
		this.ext = ext;
	}

	public List<String> getProfile() {
		return profile;
	}

	public void setProfile(List<String> profile) {
		this.profile = profile;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}
}
