package com.github.jasminb.jsonapi.retrofit;

import com.github.jasminb.jsonapi.ResourceConverter;
import okhttp3.ResponseBody;
import retrofit2.Converter;

import java.io.IOException;

/**
 * JSON API response body converter.
 *
 * @author jbegic
 */
public class JSONAPIResponseBodyConverter<T> implements Converter<ResponseBody, T> {
	private final Class<?> clazz;
	private final Boolean isCollection;
	private final ResourceConverter parser;

	public JSONAPIResponseBodyConverter(ResourceConverter parser, Class<?> clazz, boolean isCollection) {
		this.clazz = clazz;
		this.isCollection = isCollection;
		this.parser = parser;
	}

	@Override
	public T convert(ResponseBody responseBody) throws IOException {
		if (isCollection) {
			return (T) parser.readDocumentCollection(responseBody.bytes(), clazz).get();
		} else {
			return (T) parser.readDocument(responseBody.bytes(), clazz).get();
		}
	}
}
