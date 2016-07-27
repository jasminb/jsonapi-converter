package com.github.jasminb.jsonapi.retrofit;

import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * JSON API response body converter.
 *
 * @author jbegic
 */
public class JSONAPIDocumentResponseBodyConverter<T> implements Converter<ResponseBody, JSONAPIDocument<T>> {
	private final Class<?> clazz;
	private final Boolean isCollection;
	private final ResourceConverter parser;

	/**
	 * Creates new JSONAPIDocumentResponseBodyConverter.
	 * @param parser {@link ResourceConverter} parser instance
	 * @param clazz {@link Class} class to be handled
	 * @param isCollection {@link Boolean} flag that denotes if processed resource is a single object or collection
	 */
	public JSONAPIDocumentResponseBodyConverter(ResourceConverter parser, Class<?> clazz, boolean isCollection) {
		this.clazz = clazz;
		this.isCollection = isCollection;
		this.parser = parser;
	}

	@Override
	public JSONAPIDocument<T> convert(ResponseBody responseBody) throws IOException {
		if (isCollection) {
			return (JSONAPIDocument<T>) parser.readDocumentCollection(responseBody.byteStream(), clazz);
		} else {
			return (JSONAPIDocument<T>) parser.readDocument(responseBody.byteStream(), clazz);
		}
	}
}
