package com.github.jasminb.jsonapi.retrofit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.ResourceConverter;
import com.github.jasminb.jsonapi.models.errors.Error;
import com.github.jasminb.jsonapi.ErrorUtils;
import com.github.jasminb.jsonapi.IOUtils;
import com.github.jasminb.jsonapi.models.User;
import com.github.jasminb.jsonapi.models.errors.Errors;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.List;

/**
 * Retrofit plugin tests.
 *
 * @author jbegic
 */
public class RetrofitTest {
	private ResourceConverter converter;
	private MockWebServer server;
	private SimpleService service;

	@Before
	public void setup() throws IOException {
		// Setup server
		server = new MockWebServer();
		server.start();

		// Setup retrofit
		converter = new ResourceConverter(User.class);
		JSONAPIConverterFactory converterFactory = new JSONAPIConverterFactory(converter);

		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(server.url("/").toString())
				.addConverterFactory(converterFactory)
				.build();

		service = retrofit.create(SimpleService.class);
	}

	@After
	public void destroy() throws IOException {
		server.shutdown();
	}

	@Test
	public void getResourceTest() throws IOException {
		String userResponse = IOUtils.getResourceAsString("user-liz.json");

		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setBody(userResponse));

		Response<User> response = service.getExampleResource().execute();

		Assert.assertTrue(response.isSuccessful());

		User user = response.body();

		Assert.assertNotNull(user);
		Assert.assertEquals("liz", user.getName());
	}

	@Test
	public void getResourceCollectionTest() throws IOException {
		String usersResponse = IOUtils.getResourceAsString("users.json");

		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setBody(usersResponse));

		Response<List<User>> response = service.getExampleResourceList().execute();

		Assert.assertTrue(response.isSuccessful());

		List<User> users = response.body();
		Assert.assertEquals(2, users.size());
	}

	@Test
	public void testError() throws IOException {
		String errorString = IOUtils.getResourceAsString("errors.json");

		server.enqueue(new MockResponse()
				.setResponseCode(400)
				.setBody(errorString));

		Response<User> response = service.getExampleResource().execute();

		Assert.assertFalse(response.isSuccessful());

		Errors errorResponse = ErrorUtils.parseErrorResponse(new ObjectMapper(), response.errorBody(), Errors.class);

		Assert.assertNotNull(errorResponse);
		Assert.assertEquals(1, errorResponse.getErrors().size());

		Error error = errorResponse.getErrors().get(0);

		Assert.assertEquals("id", error.getId());
		Assert.assertEquals("status", error.getStatus());
		Assert.assertEquals("code", error.getCode());
		Assert.assertEquals("title", error.getTitle());
		Assert.assertEquals("about", error.getLinks().getAbout());
		Assert.assertEquals("title", error.getTitle());
		Assert.assertEquals("pointer", error.getSource().getPointer());
		Assert.assertEquals("detail", error.getDetail());

		// Shutdown server
		server.shutdown();
	}

	@Test(expected = IllegalArgumentException.class)
	public void getUnregisteredResourceTest() throws IOException {
		String userResponse = IOUtils.getResourceAsString("errors.json");

		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setBody(userResponse));

		service.getNonJSONSPECResource().execute();
	}

	@Test
	public void testRequestParsing() throws Exception {
		String userResponse = IOUtils.getResourceAsString("user-liz.json");


		server.enqueue(new MockResponse()
				.setResponseCode(201)
				.setBody(userResponse));

		User user = new User();
		user.setId("id");
		user.setName("name");

		User response = service.createUser(user).execute().body();

		Assert.assertEquals("liz", response.getName());

		RecordedRequest request = server.takeRequest();

		String requestBody = new String(request.getBody().readByteArray());

		Assert.assertEquals(new String(converter.writeObject(user)), requestBody);
	}
}
