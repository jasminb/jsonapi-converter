package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.jasminb.jsonapi.models.Article;
import com.github.jasminb.jsonapi.models.Author;
import com.github.jasminb.jsonapi.models.Comment;
import com.github.jasminb.jsonapi.models.NoIdAnnotationModel;
import com.github.jasminb.jsonapi.models.RecursingNode;
import com.github.jasminb.jsonapi.models.Status;
import com.github.jasminb.jsonapi.models.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Testing functionality of JSON API converter.
 *
 * @author jbegic
 */
public class ResourceConverterTest {
	private ResourceConverter converter;

	@Before
	public void setup() {
		converter = new ResourceConverter(Status.class, User.class);
	}

	@Test
	public void testReadWriteObject() throws Exception {
		Status status = new Status();
		status.setContent("content");
		status.setCommentCount(1);
		status.setLikeCount(10);
		status.setId("id");
		status.setUser(new User());
		status.getUser().setId("userid");
		status.setRelatedUser(status.getUser());

		byte [] rawData = converter.writeObject(status);

		Assert.assertNotNull(rawData);
		Assert.assertFalse(rawData.length == 0);

		Status converted = converter.readObject(rawData, Status.class);

		// Make sure relationship with disabled serialisation is not present
		Assert.assertNull(converted.getRelatedUser());

		Assert.assertEquals(status.getId(), converted.getId());
		Assert.assertEquals(status.getLikeCount(), converted.getLikeCount());
		Assert.assertEquals(status.getCommentCount(), converted.getCommentCount());
		Assert.assertEquals(status.getContent(), converted.getContent());


		Assert.assertNotNull(converted.getUser());
		Assert.assertEquals(status.getUser().getId(), converted.getUser().getId());
	}

	@Test
	public void testReadWithIncludedSection() throws IOException {
		String apiResponse = IOUtils.getResourceAsString("status.json");

		Status status = converter.readObject(apiResponse.getBytes(), Status.class);

		Assert.assertNotNull(status.getUser());
		Assert.assertEquals("john", status.getUser().getName());

		Assert.assertNotNull(status.getUser().getStatuses());
		Assert.assertEquals(2, status.getUser().getStatuses().size());
	}

	@Test
	public void testWriteCollection() throws IOException, IllegalAccessException {
		String usersRequest = IOUtils.getResourceAsString("users.json");

		List<User> users = converter.readObjectCollection(usersRequest.getBytes(), User.class);

		byte[] convertedData = converter.writeObjectCollection(users);

		Assert.assertNotNull(convertedData);
		Assert.assertFalse(convertedData.length == 0);

		List<User> converted = converter.readObjectCollection(convertedData, User.class);

		Assert.assertNotNull(converted);
		Assert.assertEquals(users.size(), converted.size());
		Assert.assertEquals(usersRequest.replaceAll("\\s",""), new String(convertedData).replaceAll("\\s",""));
	}

	@Test
	public void testReadWithMetaAndLinksSection() throws IOException {
		String apiResponse = IOUtils.getResourceAsString("user-with-meta.json");

		JSONAPIDocument<User> document = converter.readDocument(apiResponse.getBytes(), User.class);

		Assert.assertNotNull(document.getMeta());
		Assert.assertEquals("asdASD123", document.getMeta().get("token"));

		Assert.assertNotNull(document.get().getMeta());
		Assert.assertEquals("asdASD123", document.get().getMeta().getToken());

		// Assert top level link data
		Assert.assertNotNull(document.getLinks());
		Assert.assertEquals("href", document.getLinks().getRelated().getHref());
		Assert.assertEquals(10, document.getLinks().getRelated().getMeta().get("count"));

		// Assert document level link data
		Assert.assertNotNull(document.get().links);
		Assert.assertEquals("href", document.get().links.getRelated().getHref());
		Assert.assertEquals(10, document.get().links.getRelated().getMeta().get("count"));
	}

	@Test
	public void testWriteWithMetaSection() throws IOException, IllegalAccessException {
		User initialUser;
		User.UserMeta userMeta = new User.UserMeta();
		userMeta.token = "test";
		initialUser = new User();
		initialUser.meta = userMeta;
		initialUser.id = "123";
		initialUser.name = "John Nash";

		byte [] rawData = converter.writeObject(initialUser);

		Assert.assertNotNull(rawData);
		Assert.assertFalse(rawData.length == 0);

		User converted = converter.readObject(rawData, User.class);

		Assert.assertEquals(null, converted.getMeta());
	}

	@Test
	public void testResolverGlobal() throws IOException {
		converter.setGlobalResolver(new RelationshipResolver() {
			@Override
			public byte[] resolve(String relationshipURL) {
				try {
					return IOUtils.getResourceAsString("user-liz.json").getBytes();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});

		String apiResponse = IOUtils.getResourceAsString("status.json");

		Status status = converter.readObject(apiResponse.getBytes(), Status.class);

		Assert.assertNotNull(status.getUser());
		Assert.assertEquals("liz", status.getUser().getName());
	}

	@Test
	public void testResolverTyped() throws IOException {
		converter.setGlobalResolver(new RelationshipResolver() {
			@Override
			public byte[] resolve(String relationshipURL) {
				try {
					return IOUtils.getResourceAsString("user-liz.json").getBytes();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});

		converter.setTypeResolver(new RelationshipResolver() {
			@Override
			public byte[] resolve(String relationshipURL) {
				try {
					return IOUtils.getResourceAsString("user-john.json").getBytes();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}, User.class);

		String apiResponse = IOUtils.getResourceAsString("status.json");

		Status status = converter.readObject(apiResponse.getBytes(), Status.class);

		Assert.assertNotNull(status.getUser());
		Assert.assertEquals("john", status.getUser().getName());
	}

	@Test
	public void testReadCollection() throws IOException {
		String apiResponse = IOUtils.getResourceAsString("users.json");

		List<User> users = converter.readObjectCollection(apiResponse.getBytes(), User.class);

		Assert.assertEquals(2, users.size());

		Assert.assertEquals("1", users.get(0).getId());
		Assert.assertEquals("liz", users.get(0).getName());

		Assert.assertEquals("2", users.get(1).getId());
		Assert.assertEquals("john", users.get(1).getName());

	}

	@Test
	public void testReadWithCollectionRelationship() throws IOException {
		String apiResponse = IOUtils.getResourceAsString("user-with-statuses.json");

		User user = converter.readObject(apiResponse.getBytes(), User.class);

		Assert.assertNotNull(user.getStatuses());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExpectCollection() throws IOException {
		converter.readObjectCollection(IOUtils.getResourceAsString("user-with-statuses.json").getBytes(), User.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExpectObject() throws IOException {
		converter.readObject(IOUtils.getResourceAsString("users.json").getBytes(), User.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExpectData() {
		converter.readObject("{}".getBytes(), User.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDataNodeMustBeAnObject() {
		converter.readObject("{\"data\" : \"attribute\"}".getBytes(), User.class);
	}

	@Test
	public void testIncludedFullRelationships() throws IOException {
		String apiResponse = IOUtils.getResourceAsString("articles.json");

		ObjectMapper articlesMapper = new ObjectMapper();
		articlesMapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

		ResourceConverter articlesConverter = new ResourceConverter(articlesMapper, Article.class, Author.class,
				Comment.class);

		List<Article> articles = articlesConverter.readObjectCollection(apiResponse.getBytes(), Article.class);


		Assert.assertNotNull(articles);
		Assert.assertEquals(1, articles.size());

		Article article = articles.get(0);

		Assert.assertEquals("JSON API paints my bikeshed!", article.getTitle());
		Assert.assertEquals("1", article.getId());

		Assert.assertNotNull(article.getAuthor());

		Author author = article.getAuthor();

		Assert.assertEquals("9", author.getId());
		Assert.assertEquals("Dan", author.getFirstName());

		Assert.assertNotNull(article.getComments());

		List<Comment> comments = article.getComments();

		Assert.assertEquals(2, comments.size());

		Comment commentWithAuthor = comments.get(1);

		Assert.assertEquals("12", commentWithAuthor.getId());
		Assert.assertEquals("I like XML better", commentWithAuthor.getBody());

		Assert.assertNotNull(commentWithAuthor.getAuthor());
		Assert.assertEquals("9", commentWithAuthor.getAuthor().getId());
		Assert.assertEquals("dgeb", commentWithAuthor.getAuthor().getTwitter());
	}

	@Test
	public void testReadWithCollectionInvalidRelationships() throws IOException {
		String apiResponse = IOUtils.getResourceAsString("user-with-invalid-relationships.json");

		User user = converter.readObject(apiResponse.getBytes(), User.class);

		Assert.assertNotNull(user.getStatuses());
		Assert.assertFalse(user.getStatuses().isEmpty());

		Assert.assertEquals("valid", user.getStatuses().get(0).getId());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUsingNoTypeAnnotationClass() {
		new ResourceConverter(String.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUsingNoIdAnnotationClass() {
		new ResourceConverter(NoIdAnnotationModel.class);
	}

	@Test
	public void testLinkObjectsAndRelType() throws Exception {
		ObjectMapper articlesMapper = new ObjectMapper();

		String apiResponse = IOUtils.getResourceAsString("articles-with-link-objects.json");
		articlesMapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

		// Configure the ProbeResolver
		Map<String, String> responseMap = new HashMap<>();
		String authorRel = "http://example.com/articles/1/author";
		String commentRel = "http://example.com/articles/1/relationships/comments";
		responseMap.put(authorRel,
				IOUtils.getResourceAsString("author-reltype-related-response.json"));
		responseMap.put(commentRel,
				IOUtils.getResourceAsString("comment-reltype-self-response.json"));
		ProbeResolver resolver = new ProbeResolver(responseMap);

		// Configure the ResourceConverter with the ProbeResolver
		ResourceConverter underTest = new ResourceConverter(articlesMapper, Article.class, Author.class,
				Comment.class);
		underTest.setGlobalResolver(resolver);

		JSONAPIDocument<List<Article>> responseDocument = underTest
				.readDocumentCollection(apiResponse.getBytes(), Article.class);
		Assert.assertNotNull(responseDocument);
		List<Article> articles = responseDocument.get();

		// Sanity check
		Assert.assertNotNull(articles);
		Assert.assertTrue(articles.size() > 0);

		// Assert relationships were resolved
		Assert.assertEquals(1, resolver.resolved.get(authorRel).intValue());
		Assert.assertEquals(1, resolver.resolved.get(commentRel).intValue());
	}


	@Test
	public void testRelationshipResolutionRecursionLoop() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

		String loopUrl = "http://example.com/node/1";
		String loopJson = IOUtils.getResourceAsString("recursion.json");

		// Configure the ProbeResolver
		Map<String, String> responseMap = new HashMap<>();
		responseMap.put(loopUrl, loopJson);
		ProbeResolver resolver = new ProbeResolver(responseMap);

		// Configure the ResourceConverter with the ProbeResolver
		ResourceConverter underTest = new ResourceConverter(mapper, RecursingNode.class);
		underTest.setGlobalResolver(resolver);

		RecursingNode p = underTest.readObject(loopJson.getBytes(), RecursingNode.class);

		// Sanity check
		Assert.assertNotNull(p);

		// Verify
		Assert.assertEquals(1, resolver.resolved.get(loopUrl).intValue());
		Assert.assertNotNull(p.getParent());
	}

	/**
	 * The JSON {@code null} value carries semantics, as in pagination links:
	 * <pre>
	 * "links": {
	 *   "first": null,
	 *   "last": "https://test-api.osf.io/v2/nodes/?page=9",
	 *   "prev": null,
	 *   "next": "https://test-api.osf.io/v2/nodes/?page=2",
	 * }
	 * </pre>
	 * It is important that {@code null} values be reflected in mapped data.  This test insures that {@code null} JSON
	 * values are mapped to Java {@code null}, and not to a string value representing {@code null} (which is the
	 * default behavior of the Jackson {@code NullNode}).
	 * @throws Exception
     */
	@Test
	public void testGetLinkNullity() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String link = "http://example.com/resource/1";
		JsonNode linkNode = mapper.readTree("\"" + link + "\"");
		Assert.assertEquals(link, converter.getLink(linkNode));
		linkNode = mapper.readTree("null");
		Assert.assertNull(converter.getLink(linkNode));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEnforceIdDeserializationOption() throws IOException {
		String rawData = IOUtils.getResourceAsString("user-john-no-id.json");
		converter.readDocument(rawData.getBytes(StandardCharsets.UTF_8), User.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEnforceIdDeserializationOptionEmptyId() throws IOException {
		String rawData = IOUtils.getResourceAsString("user-john-empty-id.json");
		converter.readDocument(rawData.getBytes(StandardCharsets.UTF_8), User.class);
	}

	@Test
	public void testDisableEnforceIdDeserialisationOption() throws  Exception {
		converter.disableDeserializationOption(DeserializationFeature.REQUIRE_RESOURCE_ID);

		String rawData = IOUtils.getResourceAsString("user-john-no-id.json");
		User user = converter.readDocument(rawData.getBytes(StandardCharsets.UTF_8), User.class).get();
		Assert.assertNotNull(user);
		Assert.assertEquals("john", user.getName());
	}

	/**
	 * Simple global RelationshipResolver implementation that maintains a count of responses for each
	 * relationship url.
	 */
	private class ProbeResolver implements RelationshipResolver {

		/**
		 * Map of relationship urls to the response JSON
		 */
		private Map<String, String> responseMap;

		/**
		 * Map of relationship to a count of the times they have been resolved
		 */
		private Map<String, Integer> resolved = new HashMap<>();

		ProbeResolver(Map<String, String> responseMap) {
			this.responseMap = responseMap;
		}

		@Override
		public byte[] resolve(String relationshipURL) {
			if (responseMap.containsKey(relationshipURL)) {
				if (resolved.containsKey(relationshipURL)) {
					int count = resolved.get(relationshipURL);
					resolved.put(relationshipURL, ++count);
				} else {
					resolved.put(relationshipURL, 1);
				}
				return responseMap.get(relationshipURL).getBytes();
			}
			throw new IllegalArgumentException("Unable to resolve '" + relationshipURL + "', missing response map " +
					"entry.");
		}
	}
}
