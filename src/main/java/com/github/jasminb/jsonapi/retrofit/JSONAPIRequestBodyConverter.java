package com.github.jasminb.jsonapi.retrofit;

import com.github.jasminb.jsonapi.ResourceConverter;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import retrofit.Converter;

import java.io.IOException;

/**
 * JSON API request body converter implementation.
 *
 * @author jbegic
 */
public class JSONAPIRequestBodyConverter<T> implements Converter<T, RequestBody> {
	private ResourceConverter converter;

	public JSONAPIRequestBodyConverter(ResourceConverter converter) {
		this.converter = converter;
	}


	@Override
	public RequestBody convert(T t) throws IOException {
		try {
			MediaType mediaType = MediaType.parse("application/vnd.api+json");
			return RequestBody.create(mediaType, converter.writeObject(t));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
