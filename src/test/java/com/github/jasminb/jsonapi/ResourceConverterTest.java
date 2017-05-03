package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.jasminb.jsonapi.exceptions.DocumentSerializationException;
import com.github.jasminb.jsonapi.models.Article;
import com.github.jasminb.jsonapi.models.Author;
import com.github.jasminb.jsonapi.models.Comment;
import com.github.jasminb.jsonapi.models.NoDefaultConstructorClass;
import com.github.jasminb.jsonapi.models.NoIdAnnotationModel;
import com.github.jasminb.jsonapi.models.RecursingNode;
import com.github.jasminb.jsonapi.models.SimpleMeta;
import com.github.jasminb.jsonapi.models.Status;
import com.github.jasminb.jsonapi.models.User;
import com.github.jasminb.jsonapi.models.inheritance.BaseModel;
import com.github.jasminb.jsonapi.models.inheritance.City;
import com.github.jasminb.jsonapi.models.inheritance.Engineer;
import com.github.jasminb.jsonapi.models.inheritance.EngineeringField;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
		converter = new ResourceConverter("https://api.example.com", Status.class, User.class, Author.class,
				Article.class, Comment.class, Engineer.class, EngineeringField.class, City.class,
				NoDefaultConstructorClass.class);
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

		byte [] rawData = converter.writeDocument(new JSONAPIDocument<>(status));
		
		Assert.assertNotNull(rawData);
		Assert.assertFalse(rawData.length == 0);

		JSONAPIDocument<Status> convertedDocument = converter.readDocument(new ByteArrayInputStream(rawData), Status.class);
		Status converted = convertedDocument.get();
		// Make sure relationship with disabled serialisation is not present
		Assert.assertNull(converted.getRelatedUser());

		Assert.assertEquals(status.getId(), converted.getId());
		Assert.assertEquals(status.getLikeCount(), converted.getLikeCount());
		Assert.assertEquals(status.getCommentCount(), converted.getCommentCount());
		Assert.assertEquals(status.getContent(), converted.getContent());


		Assert.assertNotNull(converted.getUser());
		Assert.assertEquals(status.getUser().getId(), converted.getUser().getId());
		
		// Make sure type link is present
		Assert.assertNotNull(converted.getLinks());
		Assert.assertEquals("https://api.example.com/statuses/id",
				converted.getLinks().getSelf().getHref());
		
		// Make sure relationship links are present
		Assert.assertNotNull(converted.getUserRelationshipLinks());
		Assert.assertEquals("https://api.example.com/statuses/id/relationships/user",
				converted.getUserRelationshipLinks().getSelf().getHref());
		Assert.assertEquals("https://api.example.com/statuses/id/user",
				converted.getUserRelationshipLinks().getRelated().getHref());
		
	}

	@Test
	public void testReadWithIncludedSection() throws IOException {
		InputStream apiResponse = IOUtils.getResource("status.json");

		JSONAPIDocument<Status> statusDocument = converter.readDocument(apiResponse, Status.class);
		Status status = statusDocument.get();
		Assert.assertNotNull(status.getUser());
		Assert.assertEquals("john", status.getUser().getName());

		Assert.assertNotNull(status.getUser().getStatuses());
		Assert.assertEquals(2, status.getUser().getStatuses().size());
	}

	@Test
	public void testWriteCollection() throws IOException, IllegalAccessException {
		InputStream usersRequest = IOUtils.getResource("users.json");

		JSONAPIDocument<List<User>> usersDocument = converter.readDocumentCollection(usersRequest, User.class);
		List<User> users = usersDocument.get();
		byte[] convertedData = converter.writeObjectCollection(users);

		Assert.assertNotNull(convertedData);
		Assert.assertFalse(convertedData.length == 0);

		JSONAPIDocument<List<User>> convertedDocument = converter.readDocumentCollection(new ByteArrayInputStream(convertedData), User.class);
		List<User> converted = convertedDocument.get();
		Assert.assertNotNull(converted);

		Assert.assertEquals(users.size(), converted.size());
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode node1 = mapper.readTree(IOUtils.getResource("users.json"));
			JsonNode node2 = mapper.readTree(convertedData);
			Assert.assertEquals(node1, node2);
		} catch (IOException e) {
			throw new RuntimeException("Unable to read json, make sure is correct", e);
		}
	}

	@Test
	public void testReadWithMetaAndLinksSection() throws IOException {
		InputStream apiResponse = IOUtils.getResource("user-with-meta.json");

		JSONAPIDocument<User> document = converter.readDocument(apiResponse, User.class);

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

		// Assert typed meta
		SimpleMeta meta = document.getMeta(SimpleMeta.class);
		Assert.assertEquals("asdASD123", meta.getToken());

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
		
		
		converter.disableSerializationOption(SerializationFeature.INCLUDE_META);
		byte [] rawData = converter.writeObject(initialUser);

		Assert.assertNotNull(rawData);
		Assert.assertFalse(rawData.length == 0);
		
		JSONAPIDocument<User> converted = converter.readDocument(new ByteArrayInputStream(rawData), User.class);
		Assert.assertEquals(null, converted.get().getMeta());
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

		InputStream apiResponse = IOUtils.getResource("status.json");

		JSONAPIDocument<Status> statusDocument = converter.readDocument(apiResponse, Status.class);
		Status status = statusDocument.get();
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

		InputStream apiResponse = IOUtils.getResource("status.json");

		JSONAPIDocument<Status> statusDocument = converter.readDocument(apiResponse, Status.class);
		Status status = statusDocument.get();

		Assert.assertNotNull(status.getUser());
		Assert.assertEquals("john", status.getUser().getName());
	}

	@Test
	public void testReadCollection() throws IOException {
		InputStream apiResponse = IOUtils.getResource("users.json");

		JSONAPIDocument<List<User>> usersDocument = converter.readDocumentCollection(apiResponse, User.class);
		List<User> users = usersDocument.get();

		Assert.assertEquals(2, users.size());

		Assert.assertEquals("1", users.get(0).getId());
		Assert.assertEquals("liz", users.get(0).getName());

		Assert.assertEquals("2", users.get(1).getId());
		Assert.assertEquals("john", users.get(1).getName());

	}

	@Test
	public void testReadWithCollectionRelationship() throws IOException {
		InputStream apiResponse = IOUtils.getResource("user-with-statuses.json");

		JSONAPIDocument<User> userDocument = converter.readDocument(apiResponse, User.class);
		User user = userDocument.get();
		Assert.assertNotNull(user.getStatuses());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testExpectData() throws UnsupportedEncodingException {
		converter.readDocument(new ByteArrayInputStream("{}".getBytes()), User.class);
	}

	@Test
	public void testIncludedFullRelationships() throws IOException {
		InputStream apiResponse = IOUtils.getResource("articles.json");

		ObjectMapper articlesMapper = new ObjectMapper();
		articlesMapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

		ResourceConverter articlesConverter = new ResourceConverter(articlesMapper, Article.class, Author.class,
				Comment.class);

		JSONAPIDocument<List<Article>> articlesDocument = articlesConverter.readDocumentCollection(apiResponse, Article.class);
		List<Article> articles = articlesDocument.get();

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
		InputStream apiResponse = IOUtils.getResource("user-with-invalid-relationships.json");

		JSONAPIDocument<User> userDocument = converter.readDocument(apiResponse, User.class);
		User user = userDocument.get();
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

		InputStream apiResponse = IOUtils.getResource("articles-with-link-objects.json");
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

		List<Article> articles = underTest.readDocumentCollection(apiResponse, Article.class).get();

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

		RecursingNode p = underTest.readDocument(new ByteArrayInputStream(loopJson.getBytes()), RecursingNode.class).get();

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
		InputStream rawData = IOUtils.getResource("user-john-no-id.json");
		converter.readDocument(rawData, User.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEnforceIdDeserializationOptionEmptyId() throws IOException {
		InputStream rawData = IOUtils.getResource("user-john-empty-id.json");
		converter.readDocument(rawData, User.class);
	}

	@Test
	public void testDisableEnforceIdDeserialisationOption() throws  Exception {
		converter.disableDeserializationOption(DeserializationFeature.REQUIRE_RESOURCE_ID);

		InputStream rawData = IOUtils.getResource("user-john-no-id.json");
		User user = converter.readDocument(rawData, User.class).get();
		Assert.assertNotNull(user);
		Assert.assertEquals("john", user.getName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDisallowUnknownInclusionsByDefault() throws IOException {
		InputStream rawData = IOUtils.getResource("unknown-inclusions.json");
		converter.readDocument(rawData, User.class).get();
	}

	@Test
	public void testEnableAllowUnknownInclusions() throws IOException {
		converter.enableDeserializationOption(DeserializationFeature.ALLOW_UNKNOWN_INCLUSIONS);

		InputStream rawData = IOUtils.getResource("unknown-inclusions.json");
		Status status = converter.readDocument(rawData, Status.class).get();

		Assert.assertNotNull(status);
		Assert.assertEquals("content", status.getContent());
		Assert.assertEquals("john", status.getUser().getName());

		// Get and check the related statuses for the user
		List<Status> statuses = status.getUser().getStatuses();
		Assert.assertNotNull(statuses);
		Assert.assertEquals(2, statuses.size());

		for (Status relatedStatus : statuses) {
			if (relatedStatus.getId().equals("myid")) {
				Assert.assertEquals("myContent", relatedStatus.getContent());
			} else if (relatedStatus.getId().equals("anotherid")) {
				Assert.assertEquals("anotherContent", relatedStatus.getContent());
			} else {
				Assert.fail("Related status contain unexpected id: " + relatedStatus.getId());
			}
		}
	}

	@Test
	public void testNullDataNodeObject() {
		JSONAPIDocument<User> nullObject = converter.readDocument("{\"data\" : null}".getBytes(), User.class);
		Assert.assertNull(nullObject.get());
	}

	@Test
	public void testNullDataNodeCollection() {
		JSONAPIDocument<List<User>> nullObjectCollection = converter
				.readDocumentCollection("{\"data\" : null, \"meta\": {}}".getBytes(), User.class);
		Assert.assertTrue(nullObjectCollection.get().isEmpty());
	}

	@Test
	public void testWriteWithRelationships() throws DocumentSerializationException {
		Author author = new Author();
		author.setId("id");
		author.setFirstName("John");

		List<Comment> comments = new ArrayList<>();
		Comment comment = new Comment();
		comment.setId("id");
		comment.setBody("body");
		comment.setAuthor(author);
		comments.add(comment);

		Article article = new Article();
		article.setId("id");
		article.setTitle("title");
		article.setAuthor(author);
		article.setComments(comments);
		
		converter.enableSerializationOption(SerializationFeature.INCLUDE_RELATIONSHIP_ATTRIBUTES);
		converter.enableSerializationOption(SerializationFeature.INCLUDE_LINKS);
		
		
		byte [] serialized = converter.writeDocument(new JSONAPIDocument<>(article));
		
		JSONAPIDocument<Article> deserialized = converter.readDocument(serialized, Article.class);

		Assert.assertEquals(article.getTitle(), deserialized.get().getTitle());
		Assert.assertEquals(article.getAuthor().getFirstName(), deserialized.get().getAuthor().getFirstName());

		Assert.assertEquals(1, deserialized.get().getComments().size());
		Assert.assertEquals(comment.getBody(), deserialized.get().getComments().iterator().next().getBody());
		Assert.assertEquals(author.getFirstName(),
				deserialized.get().getComments().iterator().next().getAuthor().getFirstName());


		// Make sure that disabling serializing attributes works
		converter.disableSerializationOption(SerializationFeature.INCLUDE_RELATIONSHIP_ATTRIBUTES);

		serialized = converter.writeDocument(new JSONAPIDocument<>(article));
		deserialized = converter.readDocument(serialized, Article.class);

		Assert.assertNull(deserialized.get().getComments().iterator().next().getBody());
		Assert.assertNull(deserialized.get().getAuthor().getFirstName());

	}

	@Test
	public void testSubtypeDeserialization() throws IOException {
		InputStream data = IOUtils.getResource("engineer.json");

		JSONAPIDocument<BaseModel> engineerDocument = converter.readDocument(data, BaseModel.class);

		Assert.assertTrue(engineerDocument.get() instanceof Engineer);
	}

	@Test
	public void testNoDefCtorObjectDeserialization() throws IOException {
		String data = "{\"data\":{\"type\":\"no-def-ctor\",\"id\":\"1\",\"attributes\":{\"name\":\"Name\"}}}";

		JSONAPIDocument<NoDefaultConstructorClass> result = converter.readDocument(data.getBytes(),
				NoDefaultConstructorClass.class);

		Assert.assertEquals("Name", result.get().getName());

		result = converter.readDocument("{\"data\":{\"type\":\"no-def-ctor\",\"id\":\"1\"}}".getBytes(),
				NoDefaultConstructorClass.class);

		Assert.assertNotNull(result.get());
		Assert.assertEquals("1", result.get().getId());

	}

	@Test
	public void testWriteDocumentCollection() throws IOException, DocumentSerializationException {
		InputStream usersRequest = IOUtils.getResource("users.json");

		converter.enableSerializationOption(SerializationFeature.INCLUDE_LINKS);
		converter.enableSerializationOption(SerializationFeature.INCLUDE_META);

		JSONAPIDocument<List<User>> usersDocument = converter.readDocumentCollection(usersRequest, User.class);

		Map<String, String> meta = new HashMap<>();
		meta.put("meta", "abc");

		usersDocument.setMeta(meta);

		Map<String, Link> linkMap = new HashMap<>();
		linkMap.put("self", new Link("abc"));
		usersDocument.setLinks(new Links(linkMap));

		JSONAPIDocument<List<User>> checkDocument = converter
				.readDocumentCollection(converter.writeDocumentCollection(usersDocument), User.class);

		Assert.assertEquals(2, checkDocument.get().size());

		Assert.assertEquals(usersDocument.get().iterator().next().getId(),
				checkDocument.get().iterator().next().getId());

		Assert.assertNotNull(checkDocument.getMeta());
		Assert.assertNotNull(checkDocument.getLinks());

		Assert.assertEquals("abc", checkDocument.getLinks().getSelf().toString());
		Assert.assertEquals("abc", checkDocument.getMeta().get("meta"));
	}
	
	@Test
	public void testReadRelationshipMeta() throws IOException {
		InputStream statusStream = IOUtils.getResource("status.json");
		
		Status status = converter.readDocument(statusStream, Status.class).get();
		
		Assert.assertNotNull(status.getUserRelationshipMeta());
		Assert.assertEquals("token", status.getUserRelationshipMeta().getToken());
	}
	
	@Test
	public void testWriteRelationshipMeta() throws IOException, DocumentSerializationException {
		InputStream statusStream = IOUtils.getResource("status.json");
		JSONAPIDocument<Status> statusJSONAPIDocument = converter.readDocument(statusStream, Status.class);
		
		byte [] serialized = converter.writeDocument(statusJSONAPIDocument);
		
		Status status = converter.readDocument(serialized, Status.class).get();
		Assert.assertNotNull(status.getUserRelationshipMeta());
		Assert.assertEquals("token", status.getUserRelationshipMeta().getToken());
	}
	
	@Test
	public void testReadRelationshipLinks() throws IOException {
		InputStream statusStream = IOUtils.getResource("status.json");
		Status status = converter.readDocument(statusStream, Status.class).get();
		
		Assert.assertNotNull(status.getUserRelationshipLinks());
		Assert.assertEquals("users/userid", status.getUserRelationshipLinks().getSelf().getHref());
	}
	
	@Test
	public void testWriteRelationshipLinks() throws IOException, DocumentSerializationException {
		InputStream statusStream = IOUtils.getResource("status.json");
		JSONAPIDocument<Status> statusJSONAPIDocument = converter.readDocument(statusStream, Status.class);
		
		byte [] serialized = converter.writeDocument(statusJSONAPIDocument);
		
		Status status = converter.readDocument(serialized, Status.class).get();
		Assert.assertNotNull(status.getUserRelationshipLinks());
		Assert.assertEquals("users/userid", status.getUserRelationshipLinks().getSelf().getHref());
	}
	
	@Test
	public void testReadMetaOnly() {
		JSONAPIDocument<Status> status = converter.readDocument("{\"meta\" : {}}".getBytes(StandardCharsets.UTF_8),
				Status.class);
		
		Assert.assertNotNull(status.getMeta());
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
