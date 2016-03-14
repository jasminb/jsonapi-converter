package com.github.jsonapi.retrofit;

import com.github.jsonapi.models.User;
import retrofit.Call;
import retrofit.http.GET;

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
}
