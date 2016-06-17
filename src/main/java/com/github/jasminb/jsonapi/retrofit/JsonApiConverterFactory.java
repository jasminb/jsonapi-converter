package com.github.jasminb.jsonapi.retrofit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.Resource;
import com.github.jasminb.jsonapi.ResourceConverter;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author mushtu
 * @since 6/15/16.
 */
public class JsonApiConverterFactory extends Converter.Factory {

    private ResourceConverter parser;
    private Converter.Factory alternativeFactory;

    public JsonApiConverterFactory(ResourceConverter parser) {
        this.parser = parser;
    }

    public JsonApiConverterFactory(ObjectMapper mapper, Class<?>... classes) {
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
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        RetrofitType retrofitType = new RetrofitType(type);

        if (retrofitType.isValid() && parser.isRegisteredType(retrofitType.getType())) {
            return new JsonApiRequestBodyConverter(parser);
        } else if (alternativeFactory != null) {
            return alternativeFactory.requestBodyConverter(type,parameterAnnotations,methodAnnotations,retrofit);
        } else {
            return null;
        }
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        RetrofitType retrofitType = new RetrofitType(type);
        if(retrofitType.isValid() && !parser.isRegisteredType(retrofitType.getType()))
        {
            // register if possible
            if(Resource.class.isAssignableFrom(type.getClass()) ||
                    type.getClass().isAnnotationPresent(com.github.jasminb.jsonapi.annotations.Type.class))
                parser.registerClass(type.getClass());
        }
        if (retrofitType.isValid() && parser.isRegisteredType(retrofitType.getType())) {
            if (retrofitType.isCollection()) {
                return new JsonApiResponseBodyConverter(parser, retrofitType.getType(), true);
            } else {
                return new JsonApiResponseBodyConverter(parser, retrofitType.getType(), false);
            }
        }
        else if (alternativeFactory != null) {
            return alternativeFactory.responseBodyConverter(type,annotations,retrofit);
        } else {
            return null;
        }

    }


}
