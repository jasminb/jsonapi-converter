package com.github.jasminb.jsonapi.retrofit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * JSON API request/response converter factory.
 *
 * @author jbegic
 */
public class JSONAPIConverterFactory extends Converter.Factory {
	private ResourceConverter parser;
	private Converter.Factory alternativeFactory;

	public JSONAPIConverterFactory(ResourceConverter parser) {
		this.parser = parser;
	}

	public JSONAPIConverterFactory(ObjectMapper mapper, Class<?>... classes) {
		this.parser = new ResourceConverter(mapper, classes);
	}

	/**
	 * Sets alternative converter factory to use in case type is cannot be handled by this factory. <br />
	 *
	 * This method is useful in cases where you want to use same retrofit instance to consume primary JSON API spec
	 * APIs and some other APIs that are not JSON API spec compliant, eg. JSON.
	 * @param alternativeFactory factory implementation
	 */
	public void setAlternativeFactory(Converter.Factory alternativeFactory) {
		this.alternativeFactory = alternativeFactory;
	}

	@Override
	public Converter<ResponseBody, JSONAPIDocument<?>> responseBodyConverter(Type type, Annotation[] annotations,
																			 Retrofit retrofit) {
		RetrofitType retrofitType = new RetrofitType(type);

		if (retrofitType.isValid() && parser.isRegisteredType(retrofitType.getType())) {
			if (retrofitType.isCollection()) {
				return new JSONAPIResponseBodyConverter<>(parser, retrofitType.getType(), true);
			} else {
				return new JSONAPIResponseBodyConverter<>(parser, retrofitType.getType(), false);
			}
		} else if (alternativeFactory != null) {
			return (Converter<ResponseBody, JSONAPIDocument<?>>) alternativeFactory
					.responseBodyConverter(type, annotations, retrofit);
		} else {
			return null;
		}
	}

	@Override
	public Converter<JSONAPIDocument<?>, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
		RetrofitType retrofitType = new RetrofitType(type);

		if (retrofitType.isValid() && parser.isRegisteredType(retrofitType.getType())) {
			return new JSONAPIRequestBodyConverter<>(parser);
		} else if (alternativeFactory != null) {
			return (Converter<JSONAPIDocument<?>, RequestBody>) alternativeFactory
					.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit);
		} else {
			return null;
		}
	}
}
