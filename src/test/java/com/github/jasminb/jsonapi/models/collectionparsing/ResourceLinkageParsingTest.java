package com.github.jasminb.jsonapi.models.collectionparsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.jasminb.jsonapi.ProbeResolver;
import com.github.jasminb.jsonapi.ResourceConverter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The target of a relationship may resolve to a collection of resource objects (i.e. a {@code to-many} relationship),
 * or to a single resource object (i.e. a {@code to-one} relationship).  This test insures that the target of a
 * relationship (i.e. the result of a {@code GET} on a relationship link) can be parsed in the absence of a resource
 * linkage in the relationship object.
 * <h2>
 * Background
 * </h2>
 * The <a href="http://jsonapi.org/format/#document-top-level">spec</a> has this to say about the structure of primary
 * data in a JSON API document:
 * <pre>
 * Primary data MUST be either:
 *
 * a single resource object, a single resource identifier object, or null, for requests that target single resources
 * an array of resource objects, an array of resource identifier objects, or an empty array ([]), for requests that
 * target resource collections
 * </pre>
 * <a href="http://jsonapi.org/format/#document-resource-object-relationships">And this</a> on the structure of a
 * relationship object.  Note that the {@code resource linkage} of a relationship is optional as long as a links object
 * or a meta object are present in the relationship:
 * <pre>
 * Relationships may be to-one or to-many.
 *
 * A “relationship object” MUST contain at least one of the following:
 *
 * links: a links object containing at least one of the following:
 *   * self: a link for the relationship itself (a “relationship link”). This link allows the client to directly manipulate the relationship. For example, removing an author through an article’s relationship URL would disconnect the person from the article without deleting the people resource itself. When fetched successfully, this link returns the linkage for the related resources as its primary data. (See Fetching Relationships.)
 *   * related: a related resource link
 * data: resource linkage
 * meta: a meta object that contains non-standard meta-information about the relationship.
 * </pre>
 * This test insures that relationship resolution logic does not require the presence of a resource linkage in a
 * relationship in order to parse the resource(s) returned by dereferencing the relationship link.
 */
public class ResourceLinkageParsingTest {

    private static final String AUTHOR_ID = "9";
    private static final String AUTHOR_FIRSTNAME = "Dan";

    private static final String COMMENT_1_ID = "5";
    private static final String COMMENT_1_BODY = "First!";
    private static final String COMMENT_2_ID = "12";
    private static final String COMMENT_2_BODY = "I like XML better";

    private static final String LINK_AUTHOR_RELATED = "http://example.com/articles/1/author";
    private static final String LINK_COMMENT_RELATED = "http://example.com/articles/1/comments";

    private static final String ARTICLE_TITLE = "JSON API paints my bikeshed!";

    /**
     * This test retrieves the "author" relationship.  The relationship contains a resource linkage composed of a
     * single object.  This indicates that the relationship is a one-to-one.  Dereferencing the relationship must
     * result in a single object.
     *
     * @throws Exception
     */
    @Test
    public void testRelToObject() throws Exception {
        ObjectMapper objMapper = new ObjectMapper();
        objMapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

        ResourceConverter underTest = new ResourceConverter(objMapper,
                Article.class,
                Author.class);

        // Mock the response from the server
        Map<String, String> resolverResponse = new HashMap<>();
        resolverResponse.put(LINK_AUTHOR_RELATED, resourceAsString("author-response.json"));
        ProbeResolver resolver = new ProbeResolver(resolverResponse);
        underTest.setGlobalResolver(resolver);

        // The JSON contains a one-to-one author relationship, as indicated by the presence of a object resource linkage
        List<Article> articles = underTest.readDocumentCollection(
                resourceAsString("rel-to-object-via-linkage.json").getBytes(), Article.class).get();

        // Sanity
        Assert.assertNotNull(articles);
        Assert.assertEquals(1, resolver.getResolved().get(LINK_AUTHOR_RELATED).intValue());
        Assert.assertEquals(1, articles.size());


        // Verify we get our author by dereferencing the relationship
        Assert.assertNotNull(articles.get(0).getAuthor());
        Assert.assertEquals(ARTICLE_TITLE, articles.get(0).getTitle());
        Assert.assertEquals(AUTHOR_ID, articles.get(0).getAuthor().getId());
        Assert.assertEquals(AUTHOR_FIRSTNAME, articles.get(0).getAuthor().getFirstName());
    }

    /**
     * This test retrieves the "comments" relationship.  The relationship contains a resource linkage composed of an
     * array of objects.  This indicates that the relationship is a one-to-many.  Dereferencing the relationship must
     * result in a collection of objects.
     *
     * @throws Exception
     */
    @Test
    public void testRelToCollection() throws Exception {
        ObjectMapper objMapper = new ObjectMapper();
        objMapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

        ResourceConverter underTest = new ResourceConverter(objMapper,
                Article.class,
                Comment.class);

        // Mock response from the server
        Map<String, String> resolverResponse = new HashMap<>();
        resolverResponse.put(LINK_COMMENT_RELATED, resourceAsString("comments-response.json"));
        ProbeResolver resolver = new ProbeResolver(resolverResponse);
        underTest.setGlobalResolver(resolver);

        // The JSON contains a one-to-many comments relationship, as indicated by the presence of an array resource linkage
        List<Article> articles = underTest.readDocumentCollection(
                resourceAsString("rel-to-collection-via-linkage.json").getBytes(), Article.class).get();

        // Sanity
        Assert.assertNotNull(articles);
        Assert.assertEquals(1, articles.size());
        Assert.assertEquals(ARTICLE_TITLE, articles.get(0).getTitle());
        Assert.assertEquals(1, resolver.getResolved().get(LINK_COMMENT_RELATED).intValue());

        // Verify we got both our comments by dereferencing the relationship
        List<Comment> comments = articles.get(0).getComments();
        Assert.assertNotNull(comments);
        Assert.assertEquals(2, comments.size());

        // Sanity
        Assert.assertEquals(COMMENT_1_ID, comments.get(0).getId());
        Assert.assertEquals(COMMENT_1_BODY, comments.get(0).getBody());
        Assert.assertEquals(COMMENT_2_ID, comments.get(1).getId());
        Assert.assertEquals(COMMENT_2_BODY, comments.get(1).getBody());
    }

    /**
     * This test retrieves the "author" relationship.  Humans understand that the relationship of an article to
     * its author is a one-to-one relationship.  However, this relationship does not contain a resource linkage; <em>no
     * conclusion can be drawn regarding the cardinality of this relationship</em>.  In the absence of a resource
     * linkage, the cardinality could be one-to-one, or one-to-many.  Recall that the presence of a resource linkage
     * is optional per the spec.
     * <p>
     * So when resolving this relationship, we don't know <em>a priori</em> whether to call
     * {@code readDocumentCollection} or {@code readDocument}.  There simply isn't enough information; we don't know
     * whether we'll receive a collection or a single object back.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testRelToObjectNoLinkage() throws Exception {
        ObjectMapper objMapper = new ObjectMapper();
        objMapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

        ResourceConverter underTest = new ResourceConverter(objMapper,
                Article.class,
                Author.class);

        // Mock response from the server
        Map<String, String> resolverResponse = new HashMap<>();
        resolverResponse.put(LINK_AUTHOR_RELATED, resourceAsString("author-response.json"));
        ProbeResolver resolver = new ProbeResolver(resolverResponse);
        underTest.setGlobalResolver(resolver);

        // The JSON contains no information regarding the cardinality of the relationship.  It does not possess a
        // resource linkage.  Resource linkages are optional per the spec.
        List<Article> articles = underTest.readDocumentCollection(
                resourceAsString("rel-to-object-no-linkage.json").getBytes(), Article.class).get();

        // Sanity
        Assert.assertNotNull(articles);
        Assert.assertEquals(1, articles.size());
        Assert.assertEquals(ARTICLE_TITLE, articles.get(0).getTitle());

        // Verify our collection contains a single object.
        Assert.assertNotNull(articles.get(0).getAuthor());
        Assert.assertEquals(AUTHOR_ID, articles.get(0).getAuthor().getId());
        Assert.assertEquals(AUTHOR_FIRSTNAME, articles.get(0).getAuthor().getFirstName());
    }

    /**
     * This test retrieves the "comments" relationship.  Humans understand that the relationship of an article to
     * its comments is a one-to-many relationship.  However, this relationship does not contain a resource linkage;
     * <em>no conclusion can be drawn regarding the cardinality of this relationship</em>.  In the absence of a resource
     * linkage, the cardinality could be one-to-one, or one-to-many.  Recall that the presence of a resource linkage
     * is optional per the spec.
     * <p>
     * So when resolving this relationship, we don't know <em>a priori</em> whether to call
     * {@code readDocumentCollection} or {@code readDocument}.  There simply isn't enough information; we don't know
     * whether we'll receive a collection or a single object back.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testRelToCollectionNoLinkage() throws Exception {
        ObjectMapper objMapper = new ObjectMapper();
        objMapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

        ResourceConverter underTest = new ResourceConverter(objMapper,
                Article.class,
                Comment.class);

        // Mock response from the server
        Map<String, String> resolverResponse = new HashMap<>();
        resolverResponse.put(LINK_COMMENT_RELATED, resourceAsString("comments-response.json"));
        ProbeResolver resolver = new ProbeResolver(resolverResponse);
        underTest.setGlobalResolver(resolver);

        // The JSON contains no information regarding the cardinality of the relationship.  It does not possess a
        // resource linkage.  Resource linkages are optional per the spec.
        List<Article> articles = underTest.readDocumentCollection(
                resourceAsString("rel-to-collection-no-linkage.json").getBytes(), Article.class).get();

        // Sanity
        Assert.assertNotNull(articles);
        Assert.assertEquals(1, articles.size());
        Assert.assertEquals(ARTICLE_TITLE, articles.get(0).getTitle());
        Assert.assertEquals(1, resolver.getResolved().get(LINK_COMMENT_RELATED).intValue());

        // Verify we get both comments by dereferencing the relationship
        List<Comment> comments = articles.get(0).getComments();
        Assert.assertNotNull(comments);
        Assert.assertEquals(2, comments.size());

        // Sanity
        Assert.assertEquals(COMMENT_1_ID, comments.get(0).getId());
        Assert.assertEquals(COMMENT_1_BODY, comments.get(0).getBody());
        Assert.assertEquals(COMMENT_2_ID, comments.get(1).getId());
        Assert.assertEquals(COMMENT_2_BODY, comments.get(1).getBody());
    }

    private static String resourceAsString(String resource) {
        URL url = ResourceLinkageParsingTest.class.getResource(resource);
        Assert.assertNotNull("Could not resolve resource '" + resource + "' on the classpath.", url);
        try {
            return org.apache.commons.io.IOUtils.toString(url, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
