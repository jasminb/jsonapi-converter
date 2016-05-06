package com.github.jasminb.jsonapi.resolutionstrategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.jasminb.jsonapi.ProbeResolver;
import com.github.jasminb.jsonapi.ResourceConverter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Insures that the @Relationship attribute {@code relationshipStrategy} is properly used to resolve relationship links
 * to objects ({@code ResolutionStrategy#OBJECT}) or to String references ({@code ResolutionStrategy#REF)}.
 */
public class ResolutionStrategyTest {

    private static final String COMMENTS_REL_LINK = "http://example.com/articles/1/comments";

    private static final String COMMENT_1_ID = "5";

    private static final String COMMENT_1_BODY = "First!";

    private static final String COMMENT_1_AUTHOR_REL_LINK = "http://example.com/comments/5/author";

    private static final String COMMENT_2_ID = "12";

    private static final String COMMENT_2_BODY = "I like XML better";

    private static final String COMMENT_2_AUTHOR_REL_LINK = "http://example.com/comments/12/author";

    private static final String ARTICLES_AUTHOR_REL_LINK = "http://example.com/articles/1/author";


    /**
     * Insures that {@link com.github.jasminb.jsonapi.ResolutionStrategy} is properly used to resolve objects or string
     * references.  In this test, the {@code Article} uses an object resolution strategy when resolving {@code Comment}
     * objects, and records {@code String} references (i.e. uses a reference resolution strategy) when resolving
     * {@code Author} objects.
     *
     * @throws Exception
     */
    @Test
    public void testResolutionStrategy() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

        Map<String, String> responseMap = new HashMap<>();
        responseMap.put(ARTICLES_AUTHOR_REL_LINK, "");
        responseMap.put(COMMENTS_REL_LINK, org.apache.commons.io.IOUtils.toString(
                this.getClass().getResource("comments-response.json"), "UTF-8"));
        ProbeResolver resolver = new ProbeResolver(responseMap);

        ResourceConverter underTest = new ResourceConverter(mapper, Article.class, Comment.class);
        underTest.setGlobalResolver(resolver);

        List<Article> articles = underTest.readObjectCollection(org.apache.commons.io.IOUtils.toString(
                this.getClass().getResource("articles.json"), "UTF-8").getBytes(), Article.class);

        // Sanity checks
        Assert.assertNotNull(articles);
        Assert.assertEquals(1, articles.size());
        List<Comment> comments = articles.get(0).getComments();
        Assert.assertEquals(1, resolver.getResolved().get(COMMENTS_REL_LINK).intValue());
        Assert.assertEquals(0, resolver.getResolved().get(ARTICLES_AUTHOR_REL_LINK).intValue());

        // Comments resolution strategy used objects
        Assert.assertNotNull(comments);
        Assert.assertEquals(2, comments.size());
        Assert.assertEquals(COMMENT_1_ID, comments.get(0).getId());
        Assert.assertEquals(COMMENT_1_BODY, comments.get(0).getBody());
        Assert.assertEquals(COMMENT_2_ID, comments.get(1).getId());
        Assert.assertEquals(COMMENT_2_BODY, comments.get(1).getBody());

        // Author resolution strategy used references
        Assert.assertEquals(ARTICLES_AUTHOR_REL_LINK, articles.get(0).getAuthor());
        Assert.assertEquals(COMMENT_1_AUTHOR_REL_LINK, comments.get(0).getAuthor());
        Assert.assertEquals(COMMENT_2_AUTHOR_REL_LINK, comments.get(1).getAuthor());
    }

    /**
     * The {@code ResolutionStrategy#REF reference resolution strategy} requires that the field be a String.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNonStringRefField() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        ResourceConverter underTest = new ResourceConverter(mapper, Foo.class);
        underTest.setGlobalResolver(new ProbeResolver(Collections.<String, String>emptyMap()));

        underTest.readObject(org.apache.commons.io.IOUtils.toString(
                this.getClass().getResource("foo.json"), "UTF-8").getBytes(), Foo.class);
    }
}
