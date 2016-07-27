package com.github.jasminb.jsonapi.retrofit;

import com.github.jasminb.jsonapi.ResourceConverter;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;

/**
 * JSON API request body converter implementation.
 *
 * @author jbegic
 */
public class JSONAPIRequestBodyConverter<T> implements Converter<T, RequestBody> {
	private final ResourceConverter converter;

	/**
	 * Creates new JSONAPIRequestBodyConverter.
	 * @param converter {@link ResourceConverter} converter instance
	 */
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
