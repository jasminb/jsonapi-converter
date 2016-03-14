package com.github.jasminb.jsonapi;

/**
 * Relationship resolver contract. <br/>
 * Implementors of this class should provide means for invoking API using relationship URL without any
 * context awareness. Usually relationship should be resolved by simply invoking HTTP GET using provided URL.
 *
 * @author jbegic
 */
public interface RelationshipResolver {

	/**
	 * Resolve relationship data.
	 * @param relationshipURL URL. eg. <code>users/1</code> or <code>https://api.myhost.com/uers/1</code>
	 * @return raw response returned by the server (should be JSONAPI spec document.
	 */
	byte [] resolve(String relationshipURL);
}
