package com.github.jasminb.jsonapi;

/**
 * Class representing the value of of a link, which can be either a plain string URL, or an object with an 'href' and an
 * optional 'meta' field:
 *
 * "links" : { "self" : "/foo" , .. }
 *
 * or
 *
 * "links" : { "self" : { "href" : "/foo", "meta" : {} } , .. }
 *
 * @see <a href="http://jsonapi.org/format/#document-links">JSON API Spec: links section</a>
 */
public class LinkData {
	private final String href;
	private final MetaData metaData;

	public LinkData(String href) {
		this(href, null);
	}

	public LinkData(String href, MetaData metaData) {
		this.href = href;
		this.metaData = metaData;
	}

	public String getHref() {
		return href;
	}

	public MetaData getMetaData() {
		return metaData;
	}
}
