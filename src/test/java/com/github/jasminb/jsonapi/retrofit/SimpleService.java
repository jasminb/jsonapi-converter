package com.github.jasminb.jsonapi.retrofit;

import com.github.jasminb.jsonapi.models.User;
import com.github.jasminb.jsonapi.models.errors.ErrorResponse;
import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

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
	Call<ErrorResponse> getNonJSONSPECResource();

	@POST("user")
	Call<User> createUser(@Body User user);
}
