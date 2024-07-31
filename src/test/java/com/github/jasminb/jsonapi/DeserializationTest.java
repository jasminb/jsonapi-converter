package com.github.jasminb.jsonapi;

import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import com.github.jasminb.jsonapi.models.Article;
import com.github.jasminb.jsonapi.models.Author;
import com.github.jasminb.jsonapi.models.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class DeserializationTest {
	private ResourceConverter converter;

	@Before
	public void setup() {
		converter = new ResourceConverter(Article.class, Author.class);
	}

	@Test
	public void testCyclicalRelationshipDeserialization() throws IOException {
		InputStream data = IOUtils.getResource("cyclical-relationship.json");
		JSONAPIDocument<Author> deserialized = converter.readDocument(data, Author.class);

		// Get the top-level Author
		Author topLevelAuthorResource = deserialized.get();
		// Get the Author through the cyclical relationship
		Author cyclicalAuthorResource = deserialized.get().getArticles().iterator().next().getAuthor();

		// Top-level Author and cyclical relationship Author should be the same object
		Assert.assertEquals(topLevelAuthorResource, cyclicalAuthorResource);
		// Top-level Author should preserve its attributes
		Assert.assertNotNull(topLevelAuthorResource.getFirstName());
		Assert.assertNotNull(topLevelAuthorResource.getLastName());
		// Cyclical relationship Author should preserve its attributes
		Assert.assertNotNull(cyclicalAuthorResource.getFirstName());
		Assert.assertNotNull(cyclicalAuthorResource.getLastName());
	}

	@Test
	public void testJsonApiDocDeserializationWithError() {
		String doc = "{\"jsonapi\":{\"version\":\"1.0\"},\"errors\":[{\"status\":404,\"title\":\"An error\"}]}";

		try {
			converter.readDocument(doc.getBytes(), Author.class);
		} catch (ResourceParseException e) {
			Assert.assertEquals("An error", e.getErrors().getErrors().iterator().next().getTitle());
			Assert.assertEquals("1.0", e.getErrors().getJsonapi().getVersion());
		}
	}

	@Test
	public void testJsonApiDocDeserialization() throws IOException {
		InputStream data = IOUtils.getResource("user-with-jsonapi-doc.json");

		JSONAPIDocument<User> user = converter.readDocument(data, User.class);

		Assert.assertEquals("liz", user.get().getName());

		Assert.assertNotNull(user.getJsonApi());
		Assert.assertEquals("1.0", user.getJsonApi().getVersion());
		Assert.assertNotNull(user.getJsonApi().getExt());
		Assert.assertNotNull(user.getJsonApi().getProfile());
	}
}
