package com.github.jasminb.jsonapi.retrofit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.ResourceConverter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * JSON API request/response converter factory.
 *
 * @author jbegic
 */
public class JSONAPIConverterFactory extends Converter.Factory {
	private ResourceConverter deserializer;
	private ResourceConverter serializer;
	private Converter.Factory alternativeFactory;

	/**
	 * Creates new JSONAPIConverterFactory.
	 * @param converter {@link ResourceConverter}
	 */
	public JSONAPIConverterFactory(ResourceConverter converter) {
		this.deserializer = converter;
		this.serializer = converter;
	}


	/**
	 * Creates new JSONAPIConverterFactory.
	 * @param deserializer {@link ResourceConverter} converter instance to be used for deserializing responses
	 * @param serializer {@link ResourceConverter} converter instance to be used for serializing requests
	 */
	public JSONAPIConverterFactory(ResourceConverter deserializer, ResourceConverter serializer) {
		this.deserializer = deserializer;
		this.serializer = serializer;
	}

	/**
	 * Creates new JSONAPIConverterFactory.
	 * @param mapper {@link ObjectMapper} raw data mapper
	 * @param classes classes to be handled by this factory instance
	 */
	public JSONAPIConverterFactory(ObjectMapper mapper, Class<?>... classes) {
		this.deserializer = new ResourceConverter(mapper, classes);
		this.serializer = this.deserializer;
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
	public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
		RetrofitType retrofitType = new RetrofitType(type);

		if (retrofitType.isValid() && deserializer.isRegisteredType(retrofitType.getType())) {
			if (retrofitType.isJSONAPIDocumentType()) {
				return new JSONAPIDocumentResponseBodyConverter<>(deserializer, retrofitType.getType(),
						retrofitType.isCollection());
			} else {
				return new JSONAPIResponseBodyConverter<>(deserializer, retrofitType.getType(),
						retrofitType.isCollection());
			}
		} else if (alternativeFactory != null) {
			return alternativeFactory.responseBodyConverter(type, annotations, retrofit);
		} else {
			return null;
		}
	}

	@Override
	public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations,
														  Annotation[] methodAnnotations, Retrofit retrofit) {
		RetrofitType retrofitType = new RetrofitType(type);

		if (retrofitType.isValid() && deserializer.isRegisteredType(retrofitType.getType())) {
			return new JSONAPIRequestBodyConverter<>(serializer);
		} else if (alternativeFactory != null) {
			return alternativeFactory.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit);
		} else {
			return null;
		}
	}
}
