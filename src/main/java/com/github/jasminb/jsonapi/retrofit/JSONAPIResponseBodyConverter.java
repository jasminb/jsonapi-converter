package com.github.jasminb.jsonapi.retrofit;

import com.github.jasminb.jsonapi.ResourceConverter;
import com.squareup.okhttp.ResponseBody;
import retrofit.Converter;

import java.io.IOException;

/**
 * JSON API response body converter.
 *
 * @author jbegic
 */
public class JSONAPIResponseBodyConverter<T> implements Converter<ResponseBody, T> {
	private Class<?> clazz;
	private Boolean isCollection;
	private ResourceConverter parser;

	public JSONAPIResponseBodyConverter(ResourceConverter parser, Class<?> clazz, boolean isCollection) {
		this.clazz = clazz;
		this.isCollection = isCollection;
		this.parser = parser;
	}

	@Override
	public T convert(ResponseBody responseBody) throws IOException {
		if (isCollection) {
			return (T) parser.readObjectCollection(responseBody.bytes(), clazz);
		} else {
			return (T) parser.readObject(responseBody.bytes(), clazz);
		}
	}
}
