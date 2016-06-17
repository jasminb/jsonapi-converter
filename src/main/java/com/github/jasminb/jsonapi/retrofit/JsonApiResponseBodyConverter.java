package com.github.jasminb.jsonapi.retrofit;

import com.github.jasminb.jsonapi.ResourceConverter;
import okhttp3.ResponseBody;
import retrofit2.Converter;

import java.io.IOException;

/**
 * Created by mushtu on 6/16/16.
 */
public class JsonApiResponseBodyConverter<T> implements Converter<ResponseBody,T> {
    private Class<?> clazz;
    private Boolean isCollection;
    private ResourceConverter parser;

    public JsonApiResponseBodyConverter(ResourceConverter parser, Class<?> clazz, boolean isCollection)
    {
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
