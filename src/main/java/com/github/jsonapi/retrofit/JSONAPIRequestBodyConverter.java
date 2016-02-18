package com.github.jsonapi.retrofit;

import com.github.jsonapi.ResourceConverter;
import com.squareup.okhttp.RequestBody;
import retrofit.Converter;

import java.io.IOException;

/**
 * JSON API request body converter implementation.
 *
 * @author jbegic
 */
public class JSONAPIRequestBodyConverter<T> implements Converter<T, RequestBody> {
	private Class<?> clazz;
	private ResourceConverter converter;

	public JSONAPIRequestBodyConverter(ResourceConverter converter, Class<?> clazz) {
		this.clazz = clazz;
		this.converter = converter;
	}


	@Override
	public RequestBody convert(T t) throws IOException {
		try {
			return RequestBody.create(null, converter.writeObject(t));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
