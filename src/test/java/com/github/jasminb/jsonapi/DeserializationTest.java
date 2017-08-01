package com.github.jasminb.jsonapi;

import com.github.jasminb.jsonapi.models.Article;
import com.github.jasminb.jsonapi.models.Author;
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
}