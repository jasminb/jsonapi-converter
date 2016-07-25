package com.github.jasminb.jsonapi.retrofit;

import com.github.jasminb.jsonapi.models.User;
import com.github.jasminb.jsonapi.models.errors.Errors;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import java.util.List;

/**
 * Simple Retrofit service interface used for unit-tests.
 *
 * @author jbegic
 */
public interface SimpleService {

	@GET("user")
	Call<User> getExampleResource();

	@GET("users")
	Call<List<User>> getExampleResourceList();

	@GET("notanjsonapiendpoint")
	Call<Errors> getNonJSONSPECResource();

	@POST("user")
	Call<User> createUser(@Body User user);
}
