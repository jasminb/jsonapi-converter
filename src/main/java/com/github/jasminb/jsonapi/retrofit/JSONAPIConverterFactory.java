package com.github.jasminb.jsonapi.retrofit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.ResourceConverter;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import retrofit.Converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * JSON API request/response converter factory.
 *
 * @author jbegic
 */
public class JSONAPIConverterFactory extends Converter.Factory {
	private ResourceConverter parser;

	public JSONAPIConverterFactory(ResourceConverter parser) {
		this.parser = parser;
	}

	public JSONAPIConverterFactory(ObjectMapper mapper, Class<?>... classes) {
		this.parser = new ResourceConverter(mapper, classes);
	}

	@Override
	public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
		if (type instanceof ParameterizedType) {
			Type [] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
			if (typeArgs != null && typeArgs.length > 0) {
				return new JSONAPIResponseBodyConverter<>(parser, (Class<?>) typeArgs[0], true);
			}
		} else if (type instanceof Class) {
			return new JSONAPIResponseBodyConverter<>(parser, (Class<?>) type, false);
		}

		return null;
	}

	@Override
	public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
		return new JSONAPIRequestBodyConverter<>(parser);
	}
}
