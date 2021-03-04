package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.jasminb.jsonapi.exceptions.DocumentSerializationException;
import com.github.jasminb.jsonapi.exceptions.InvalidJsonApiResourceException;
import com.github.jasminb.jsonapi.exceptions.UnregisteredTypeException;
import com.github.jasminb.jsonapi.models.Article;
import com.github.jasminb.jsonapi.models.Author;
import com.github.jasminb.jsonapi.models.Car;
import com.github.jasminb.jsonapi.models.Comment;
import com.github.jasminb.jsonapi.models.Dealership;
import com.github.jasminb.jsonapi.models.IntegerIdResource;
import com.github.jasminb.jsonapi.models.LongIdResource;
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
import com.github.jasminb.jsonapi.models.inheritance.Movie;
import com.github.jasminb.jsonapi.models.inheritance.Video;
import com.github.jasminb.jsonapi.models.inheritance.Vod;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Testing functionality of JSON API converter.
 *
 * @author jbegic
 */
public class ResourceConverterTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ResourceConverter converter;

	@Before
	public void setup() {
		converter = new ResourceConverter("https://api.example.com", Status.class, User.class, Author.class,
				Article.class, Comment.class, Engineer.class, EngineeringField.class, City.class,
				IntegerIdResource.class, LongIdResource.class,
				NoDefaultConstructorClass.class, Car.class, Dealership.class);
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

		assertNotNull(rawData);
		assertFalse(rawData.length == 0);

		JSONAPIDocument<Status> convertedDocument = converter.readDocument(new ByteArrayInputStream(rawData), Status.class);
		Status converted = convertedDocument.get();
		// Make sure relationship with disabled serialisation is not present
		assertNull(converted.getRelatedUser());

		assertEquals(status.getId(), converted.getId());
		assertEquals(status.getLikeCount(), converted.getLikeCount());
		assertEquals(status.getCommentCount(), converted.getCommentCount());
		assertEquals(status.getContent(), converted.getContent());


		assertNotNull(converted.getUser());
		assertEquals(status.getUser().getId(), converted.getUser().getId());

		// Make sure type link is present
		assertNotNull(converted.getLinks());
		assertEquals("https://api.example.com/statuses/id",
				converted.getLinks().getSelf().getHref());

		// Make sure relationship links are present
		assertNotNull(converted.getUserRelationshipLinks());
		assertEquals("https://api.example.com/statuses/id/relationships/user",
				converted.getUserRelationshipLinks().getSelf().getHref());
		assertEquals("https://api.example.com/statuses/id/user",
				converted.getUserRelationshipLinks().getRelated().getHref());

	}

	@Test
	public void testReadWithIncludedSection() throws IOException {
		InputStream apiResponse = IOUtils.getResource("status.json");

		JSONAPIDocument<Status> statusDocument = converter.readDocument(apiResponse, Status.class);
		Status status = statusDocument.get();
		assertNotNull(status.getUser());
		assertEquals("john", status.getUser().getName());

		assertNotNull(status.getUser().getStatuses());
		assertEquals(2, status.getUser().getStatuses().size());
	}

	@Test
	public void testWriteCollection() throws DocumentSerializationException, IOException {
		InputStream usersRequest = IOUtils.getResource("users.json");

		JSONAPIDocument<List<User>> usersDocument = converter.readDocumentCollection(usersRequest, User.class);
		List<User> users = usersDocument.get();

		assertNotNull(users);
		assertEquals(2, users.size());

		// Make sure that relationship object i.e. statuses is null
		assertNull(users.get(0).getStatuses());

		byte[] convertedData = converter.writeDocumentCollection(usersDocument);

		assertNotNull(convertedData);
		assertFalse(convertedData.length == 0);

		JSONAPIDocument<List<User>> convertedDocument = converter.readDocumentCollection(new ByteArrayInputStream(convertedData), User.class);
		List<User> converted = convertedDocument.get();
		assertNotNull(converted);

		assertEquals(users.size(), converted.size());
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode node1 = mapper.readTree(IOUtils.getResource("users.json"));

			// Make sure relationship node always get serialized even if relationship object i.e. statuses is null
			JsonNode user1Relationships = node1.get("data").get(0).get("relationships");
			assertNotNull(user1Relationships.get("statuses"));

			JsonNode user2Relationships = node1.get("data").get(1).get("relationships");
			assertNotNull(user2Relationships.get("statuses"));

			JsonNode node2 = mapper.readTree(convertedData);

			// Make sure relationship node must always contains one of either meta, link or data node
			user1Relationships = node2.get("data").get(0).get("relationships");
			assertNotNull(user1Relationships.get("statuses"));
			assertNotNull(user1Relationships.get("statuses").get("links"));

			user2Relationships = node2.get("data").get(1).get("relationships");
			assertNotNull(user2Relationships.get("statuses"));
			assertNotNull(user2Relationships.get("statuses").get("links"));

			assertNotEquals(node1, node2);
		} catch (IOException e) {
			throw new RuntimeException("Unable to read json, make sure is correct", e);
		}
	}

	@Test
	public void testLinksForNonIncludedEmptyToManyRelationship() throws IOException, IllegalAccessException {
		InputStream apiResponse = IOUtils.getResource("articles-with-non-included-empty-to-many-relationship.json");

		ObjectMapper articlesMapper = new ObjectMapper();
		articlesMapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

		ResourceConverter articlesConverter = new ResourceConverter(articlesMapper, Article.class, Author.class,
				Comment.class);

		JSONAPIDocument<List<Article>> articlesDocument = articlesConverter.readDocumentCollection(apiResponse, Article.class);
		List<Article> articles = articlesDocument.get();

		assertNotNull(articles);
		assertEquals(1, articles.size());

		Article article = articles.get(0);

		assertNull(article.getComments());

		assertNull(article.getCommentRelationshipLinks());

		byte[] convertedData = converter.writeObjectCollection(articles);
		assertNotNull(convertedData);
		assertNotEquals(0, convertedData.length);

		JSONAPIDocument<List<Article>> convertedDocument = converter.readDocumentCollection(new ByteArrayInputStream(convertedData), Article.class);
		List<Article> convertedArticles = convertedDocument.get();
		assertNotNull(convertedArticles);

		Article convertedArticle = convertedArticles.get(0);

		assertNull(convertedArticle.getComments());

		// Make sure Relationship links are getting serialized even if relationship object i.e. comments is null
		assertNotNull(convertedArticle.getCommentRelationshipLinks());
		assertEquals("https://api.example.com/articles/1/relationships/comments", convertedArticle.getCommentRelationshipLinks().getSelf().toString());
		assertEquals("https://api.example.com/articles/1/comments", convertedArticle.getCommentRelationshipLinks().getRelated().toString());

	}

    @Test
	public void testReadWithMetaAndLinksSection() throws IOException {
		InputStream apiResponse = IOUtils.getResource("user-with-meta.json");

		JSONAPIDocument<User> document = converter.readDocument(apiResponse, User.class);

		assertNotNull(document.getMeta());
		assertEquals("asdASD123", document.getMeta().get("token"));

		assertNotNull(document.get().getMeta());
		assertEquals("asdASD123", document.get().getMeta().getToken());

		// Assert top level link data
		assertNotNull(document.getLinks());
		assertEquals("href", document.getLinks().getRelated().getHref());
		assertEquals(10, document.getLinks().getRelated().getMeta().get("count"));

		// Assert document level link data
		assertNotNull(document.get().links);
		assertEquals("href", document.get().links.getRelated().getHref());
		assertEquals(10, document.get().links.getRelated().getMeta().get("count"));

		// Assert typed meta
		SimpleMeta meta = document.getMeta(SimpleMeta.class);
		assertEquals("asdASD123", meta.getToken());

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

		assertNotNull(rawData);
		assertFalse(rawData.length == 0);

		JSONAPIDocument<User> converted = converter.readDocument(new ByteArrayInputStream(rawData), User.class);
		assertEquals(null, converted.get().getMeta());
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
		assertNotNull(status.getUser());
		assertEquals("liz", status.getUser().getName());
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

		assertNotNull(status.getUser());
		assertEquals("john", status.getUser().getName());
	}

	@Test
	public void testReadCollection() throws IOException {
		InputStream apiResponse = IOUtils.getResource("users.json");

		JSONAPIDocument<List<User>> usersDocument = converter.readDocumentCollection(apiResponse, User.class);
		List<User> users = usersDocument.get();

		assertEquals(2, users.size());

		assertEquals("1", users.get(0).getId());
		assertEquals("liz", users.get(0).getName());

		assertEquals("2", users.get(1).getId());
		assertEquals("john", users.get(1).getName());

	}

	@Test
	public void testReadCollectionInvalidItem() throws IOException {
		InputStream apiResponse = IOUtils.getResource("missing-type-collection.json");

		thrown.expect(InvalidJsonApiResourceException.class);
		thrown.expectMessage("Primary data must be an array of resource objects, an array of resource identifier objects, or an empty array ([])");

		converter.readDocumentCollection(apiResponse, User.class);
	}

	@Test
	public void testReadWithCollectionRelationship() throws IOException {
		InputStream apiResponse = IOUtils.getResource("user-with-statuses.json");

		JSONAPIDocument<User> userDocument = converter.readDocument(apiResponse, User.class);
		User user = userDocument.get();
		assertNotNull(user.getStatuses());
	}

	@Test(expected = InvalidJsonApiResourceException.class)
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

		assertNotNull(articles);
		assertEquals(1, articles.size());

		Article article = articles.get(0);

		assertEquals("JSON API paints my bikeshed!", article.getTitle());
		assertEquals("1", article.getId());

		assertNotNull(article.getAuthor());

		Author author = article.getAuthor();

		assertEquals("9", author.getId());
		assertEquals("Dan", author.getFirstName());

		assertNotNull(article.getComments());

		List<Comment> comments = article.getComments();

		assertEquals(2, comments.size());

		Comment commentWithAuthor = comments.get(1);

		assertEquals("12", commentWithAuthor.getId());
		assertEquals("I like XML better", commentWithAuthor.getBody());

		assertNotNull(commentWithAuthor.getAuthor());
		assertEquals("9", commentWithAuthor.getAuthor().getId());
		assertEquals("dgeb", commentWithAuthor.getAuthor().getTwitter());
	}

	@Test
	public void testReadWithCollectionInvalidRelationships() throws IOException {
		InputStream apiResponse = IOUtils.getResource("user-with-invalid-relationships.json");

		JSONAPIDocument<User> userDocument = converter.readDocument(apiResponse, User.class);
		User user = userDocument.get();
		assertNotNull(user.getStatuses());
		assertFalse(user.getStatuses().isEmpty());

		assertEquals("valid", user.getStatuses().get(0).getId());
	}

	@Test
	public void testReadIncludedResourceMissingType() throws IOException {
		InputStream data = IOUtils.getResource("missing-type-inclusion.json");

		thrown.expect(InvalidJsonApiResourceException.class);
		thrown.expectMessage("Included must be an array of valid resource objects, or an empty array ([])");

		converter.readDocument(data, BaseModel.class);

	}

	@Test
	public void testReadRelationshipMissingTypeInclusionIsSkipped() throws IOException {
		InputStream data = IOUtils.getResource("missing-type-relationship.json");

		JSONAPIDocument<Engineer> engineerDocument = converter.readDocument(data, Engineer.class);
		assertNull(engineerDocument.get().getCity());

	}

	@Test
	public void testReadPolymorphicRelationships() throws IOException {
		ResourceConverter carConverter = new ResourceConverter("https://api.example.com", Car.class, Dealership.class);
				InputStream apiResponse = IOUtils.getResource("cars.json");

		JSONAPIDocument<Dealership> dealershipDocument = carConverter.readDocument(apiResponse, Dealership.class);
		assertNotNull(dealershipDocument.get().getAutomobiles());
	}

	@Test
	public void testReadPolymorphicRelationshipsWithoutNewType() throws IOException {
		ResourceConverter carConverter = new ResourceConverter("https://api.example.com", Car.class, Dealership.class);
		carConverter.enableDeserializationOption(DeserializationFeature.ALLOW_UNKNOWN_INCLUSIONS);
		carConverter.enableDeserializationOption(DeserializationFeature.ALLOW_UNKNOWN_TYPE_IN_RELATIONSHIP);
		InputStream apiResponse = IOUtils.getResource("cars2.json");

		JSONAPIDocument<Dealership> dealershipDocument = carConverter.readDocument(apiResponse, Dealership.class);
		assertNotNull(dealershipDocument.get().getAutomobiles());
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
		assertNotNull(articles);
		assertTrue(articles.size() > 0);

		// Assert relationships were resolved
		assertEquals(1, resolver.resolved.get(authorRel).intValue());
		assertEquals(1, resolver.resolved.get(commentRel).intValue());
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
		assertNotNull(p);

		// Verify
		assertEquals(1, resolver.resolved.get(loopUrl).intValue());
		assertNotNull(p.getParent());
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
		assertEquals(link, converter.getLink(linkNode));
		linkNode = mapper.readTree("null");
		assertNull(converter.getLink(linkNode));
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
		assertNotNull(user);
		assertEquals("john", user.getName());
	}

	@Test(expected = UnregisteredTypeException.class)
	public void testDisallowUnknownInclusionsByDefault() throws IOException {
		InputStream rawData = IOUtils.getResource("unknown-inclusions.json");
		converter.readDocument(rawData, User.class).get();
	}

	@Test
	public void testEnableAllowUnknownInclusions() throws IOException {
		converter.enableDeserializationOption(DeserializationFeature.ALLOW_UNKNOWN_INCLUSIONS);

		InputStream rawData = IOUtils.getResource("unknown-inclusions.json");
		Status status = converter.readDocument(rawData, Status.class).get();

		assertNotNull(status);
		assertEquals("content", status.getContent());
		assertEquals("john", status.getUser().getName());

		// Get and check the related statuses for the user
		List<Status> statuses = status.getUser().getStatuses();
		assertNotNull(statuses);
		assertEquals(2, statuses.size());

		for (Status relatedStatus : statuses) {
			if (relatedStatus.getId().equals("myid")) {
				assertEquals("myContent", relatedStatus.getContent());
			} else if (relatedStatus.getId().equals("anotherid")) {
				assertEquals("anotherContent", relatedStatus.getContent());
			} else {
				fail("Related status contain unexpected id: " + relatedStatus.getId());
			}
		}
	}

	@Test
	public void testNullDataNodeObject() {
		JSONAPIDocument<User> nullObject = converter.readDocument("{\"data\" : null}".getBytes(), User.class);
		assertNull(nullObject.get());
	}

	@Test(expected = InvalidJsonApiResourceException.class)
	public void testNullDataNodeCollection() {
		JSONAPIDocument<List<User>> nullObjectCollection = converter
				.readDocumentCollection("{\"data\" : null, \"meta\": {}}".getBytes(), User.class);
		assertTrue(nullObjectCollection.get().isEmpty());
	}

	@Test
	public void testEmptyArrayDataNodeCollection() {
		JSONAPIDocument<List<User>> nullObjectCollection = converter
				.readDocumentCollection("{\"data\" : [], \"meta\": {}}".getBytes(), User.class);
		assertTrue(nullObjectCollection.get().isEmpty());
	}

	@Test
	public void testWriteWithRelationships() throws DocumentSerializationException, IOException {
		Author author = new Author();
		author.setId("id");
		author.setFirstName("John");

		List<Comment> comments = new ArrayList<>();
		Comment comment = new Comment();
		comment.setId("id");
		comment.setBody("body");
		comment.setAuthor(author);
		comments.add(comment);

		Map<String, Link> userLinkMap = new HashMap<>();
		userLinkMap.put(JSONAPISpecConstants.SELF, new Link("http://example.com/articles/id/relationships/users"));
		userLinkMap.put(JSONAPISpecConstants.RELATED, new Link("http://example.com/articles/id/users"));

		Article article = new Article();
		article.setId("id");
		article.setTitle("title");
		article.setAuthor(author);
		article.setComments(comments);
		article.setUserRelationshipLinks(new Links(userLinkMap));

		converter.enableSerializationOption(SerializationFeature.INCLUDE_RELATIONSHIP_ATTRIBUTES);
		converter.enableSerializationOption(SerializationFeature.INCLUDE_LINKS);


		byte [] serialized = converter.writeDocument(new JSONAPIDocument<>(article));

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(serialized);
		// Make sure only the relationship with disabled data serialization does not have a data node
		assertFalse(node.at("/data/relationships/author/data").isMissingNode());
		assertFalse(node.at("/data/relationships/comments/data").isMissingNode());
		assertTrue(node.at("/data/relationships/users/data").isMissingNode());

		JSONAPIDocument<Article> deserialized = converter.readDocument(serialized, Article.class);

		assertEquals(article.getTitle(), deserialized.get().getTitle());
		assertEquals(article.getAuthor().getFirstName(), deserialized.get().getAuthor().getFirstName());

		assertEquals(1, deserialized.get().getComments().size());
		assertEquals(comment.getBody(), deserialized.get().getComments().iterator().next().getBody());
		assertEquals(author.getFirstName(),
				deserialized.get().getComments().iterator().next().getAuthor().getFirstName());

		assertEquals(0, deserialized.get().getUsers().size());
		assertEquals(userLinkMap.get(JSONAPISpecConstants.SELF).toString(),
				deserialized.get().getUserRelationshipLinks().getSelf().toString());
		assertEquals(userLinkMap.get(JSONAPISpecConstants.RELATED).toString(),
				deserialized.get().getUserRelationshipLinks().getRelated().toString());

		// Make sure that disabling serializing attributes works
		converter.disableSerializationOption(SerializationFeature.INCLUDE_RELATIONSHIP_ATTRIBUTES);

		serialized = converter.writeDocument(new JSONAPIDocument<>(article));
		deserialized = converter.readDocument(serialized, Article.class);

		assertNull(deserialized.get().getComments().iterator().next().getBody());
		assertNull(deserialized.get().getAuthor().getFirstName());
	}

	/**
	 * Asserts that in cases where relationship close the loop (a -> b -> a), a is not doubly marshaled.
	 *
	 * @throws DocumentSerializationException in case serialization calls fail
	 */
	@Test
	public void testWriteWithRecursiveRelationship() throws DocumentSerializationException {
		Author author = new Author();
		author.setId("authorid");
		author.setFirstName("John");

		Author author2 = new Author();
		author2.setId("authorid");
		author2.setFirstName("John");

		Article article = new Article();
		article.setId("articleid");
		article.setTitle("title");
		article.setAuthor(author2);

		author.setArticles(Collections.singletonList(article));

		ResourceConverter converter = new ResourceConverter(Author.class, Article.class);
		converter.enableSerializationOption(SerializationFeature.INCLUDE_RELATIONSHIP_ATTRIBUTES);

		byte [] data = converter.writeDocument(new JSONAPIDocument<>(author));

		JSONAPIDocument<Author> deserialized = converter.readDocument(data, Author.class);
		assertEquals(deserialized.get(), deserialized.get().getArticles().iterator().next().getAuthor());
	}

	@Test
	public void testWriteWithKebabCaseRelationships() throws DocumentSerializationException, IOException {
		final ObjectMapper kebabMapper = new ObjectMapper();
		kebabMapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
		ResourceConverter kebabConverter = new ResourceConverter(kebabMapper, "https://api.example.com", Status.class, User.class, Author.class,
				Article.class, Comment.class, Engineer.class, EngineeringField.class, City.class,
				IntegerIdResource.class, LongIdResource.class,
				NoDefaultConstructorClass.class);
		IntegerIdResource integerId = new IntegerIdResource();
		integerId.setId(1);
		integerId.setValue("integer value");

		LongIdResource longId = new LongIdResource();
		longId.setId(2L);
		longId.setIntegerIdResource(integerId);
		longId.setValue("long value");

		byte[] kebabSerialized = kebabConverter.writeDocument(new JSONAPIDocument<>(longId));
		byte[] normalSerialized = converter.writeDocument(new JSONAPIDocument<>(longId));

		// Validate that the relationship attribute got removed in the Kebab case
		final JsonNode readBack = kebabMapper.readTree(kebabSerialized);
		assertNull(readBack.get("data").get("attributes").get("integer-id-resource"));
		// So the two serializations should be exactly the same
		assertEquals(new String(normalSerialized), new String(kebabSerialized));
	}

	@Test
	public void testSubtypeDeserialization() throws IOException {
		InputStream data = IOUtils.getResource("engineer.json");

		JSONAPIDocument<BaseModel> engineerDocument = converter.readDocument(data, BaseModel.class);

		assertTrue(engineerDocument.get() instanceof Engineer);
	}

	/**
	 * Tests use-case where API can return different types as part of its data array.
	 *
	 * This use-case is solved by introducing base-type and extending it to support different sub-types returned
	 * by the API.
	 *
	 * @throws IOException in case resource loading fails
	 */
	@Test
	public void testSubtypeCollectionDeserialization() throws IOException {
		converter.registerType(Movie.class);
		converter.registerType(Vod.class);
		InputStream data = IOUtils.getResource("subtype-list.json");

		JSONAPIDocument<List<Video>> elements = converter.readDocumentCollection(data, Video.class);

		assertEquals(2, elements.get().size());

		Movie movie = null;
		Vod vod = null;

		for (Video video : elements.get()) {
			if (video instanceof Movie) {
				movie = (Movie) video;
			} else {
				vod = (Vod) video;
			}
		}

		assertNotNull(movie);
		assertNotNull(vod);

		assertEquals("Movie Title", movie.getMovieTitle());
		assertEquals("Vod Title", vod.getVodTitle());

		assertNotNull(movie.getDescription());
		assertNotNull(vod.getDescription());
	}

	@Test
	public void testNoDefCtorObjectDeserialization() {
		String data = "{\"data\":{\"type\":\"no-def-ctor\",\"id\":\"1\",\"attributes\":{\"name\":\"Name\"}}}";

		JSONAPIDocument<NoDefaultConstructorClass> result = converter.readDocument(data.getBytes(),
				NoDefaultConstructorClass.class);

		assertEquals("Name", result.get().getName());

		result = converter.readDocument("{\"data\":{\"type\":\"no-def-ctor\",\"id\":\"1\",\"attributes\":{}}}".getBytes(),
				NoDefaultConstructorClass.class);

		assertNotNull(result.get());
		assertEquals("1", result.get().getId());

	}

	@Test
	public void testWriteDocumentCollection() throws IOException, DocumentSerializationException {
		InputStream usersRequest = IOUtils.getResource("users.json");

		converter.enableSerializationOption(SerializationFeature.INCLUDE_LINKS);
		converter.enableSerializationOption(SerializationFeature.INCLUDE_META);

		JSONAPIDocument<List<User>> usersDocument = converter.readDocumentCollection(usersRequest, User.class);

		Map<String, Object> meta = new HashMap<>();
		meta.put("meta", "abc");

		usersDocument.setMeta(meta);

		Map<String, Link> linkMap = new HashMap<>();
		linkMap.put("self", new Link("abc"));
		usersDocument.setLinks(new Links(linkMap));

		JSONAPIDocument<List<User>> checkDocument = converter
				.readDocumentCollection(converter.writeDocumentCollection(usersDocument), User.class);

		assertEquals(2, checkDocument.get().size());

		assertEquals(usersDocument.get().iterator().next().getId(),
				checkDocument.get().iterator().next().getId());

		assertNotNull(checkDocument.getMeta());
		assertNotNull(checkDocument.getLinks());

		assertEquals("abc", checkDocument.getLinks().getSelf().toString());
		assertEquals("abc", checkDocument.getMeta().get("meta"));
	}

	@Test
	public void testReadRelationshipMeta() throws IOException {
		InputStream statusStream = IOUtils.getResource("status.json");

		Status status = converter.readDocument(statusStream, Status.class).get();

		assertNotNull(status.getUserRelationshipMeta());
		assertEquals("token", status.getUserRelationshipMeta().getToken());
	}

	@Test
	public void testWriteRelationshipMeta() throws IOException, DocumentSerializationException {
		InputStream statusStream = IOUtils.getResource("status.json");
		JSONAPIDocument<Status> statusJSONAPIDocument = converter.readDocument(statusStream, Status.class);

		byte [] serialized = converter.writeDocument(statusJSONAPIDocument);

		Status status = converter.readDocument(serialized, Status.class).get();
		assertNotNull(status.getUserRelationshipMeta());
		assertEquals("token", status.getUserRelationshipMeta().getToken());
	}

	@Test
	public void testReadRelationshipLinks() throws IOException {
		InputStream statusStream = IOUtils.getResource("status.json");
		Status status = converter.readDocument(statusStream, Status.class).get();

		assertNotNull(status.getUserRelationshipLinks());
		assertEquals("users/userid", status.getUserRelationshipLinks().getSelf().getHref());
	}

	@Test
	public void testWriteRelationshipLinks() throws IOException, DocumentSerializationException {
		InputStream statusStream = IOUtils.getResource("status.json");
		JSONAPIDocument<Status> statusJSONAPIDocument = converter.readDocument(statusStream, Status.class);

		byte [] serialized = converter.writeDocument(statusJSONAPIDocument);

		Status status = converter.readDocument(serialized, Status.class).get();
		assertNotNull(status.getUserRelationshipLinks());
		assertEquals("users/userid", status.getUserRelationshipLinks().getSelf().getHref());
	}

	@Test
	public void testReadMetaOnly() {
		JSONAPIDocument<Status> status = converter.readDocument("{\"meta\" : {}}".getBytes(StandardCharsets.UTF_8),
				Status.class);

		assertNotNull(status.getMeta());
	}

	@Test
	public void testUnregisteredType() throws IOException {
		InputStream apiResponse = IOUtils.getResource("un-registered-type.json");

		thrown.expect(UnregisteredTypeException.class);
		thrown.expectMessage("No class was registered for type 'unRegisteredType'.");

		converter.readDocument(apiResponse, User.class);
	}

	@Test
	public void testInvalidDataMissingType() throws IOException {
		InputStream apiResponse = IOUtils.getResource("missing-type.json");

        thrown.expect(InvalidJsonApiResourceException.class);
		thrown.expectMessage("Primary data must be either a single resource object, a single resource identifier object, or null");

		converter.readDocument(apiResponse, User.class);
	}

	@Test
	public void testReadCollectionHasJSONNode() throws IOException {
		InputStream apiResponse = IOUtils.getResource("users.json");

		JSONAPIDocument<List<User>> documentCollection = converter.readDocumentCollection(apiResponse, User.class);
		assertNotNull(documentCollection.getResponseJSONNode());
	}

	@Test
	public void testReadDocumentHasJSONNode() throws IOException {
		InputStream apiResponse = IOUtils.getResource("status.json");

		JSONAPIDocument<Status> statusDocument = converter.readDocument(apiResponse, Status.class);
		assertNotNull(statusDocument.getResponseJSONNode());
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
