package com.github.jasminb.jsonapi.retrofit;

import com.github.jasminb.jsonapi.ResourceConverter;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;

import java.io.IOException;

/**
 * Created by mushtu on 6/16/16.
 */
public class JsonApiRequestBodyConverter<T> implements Converter<T, RequestBody> {

    private ResourceConverter converter;

    public JsonApiRequestBodyConverter(ResourceConverter converter)
    {
        this.converter = converter;
    }
    @Override
    public RequestBody convert(T value) throws IOException {
        try {
            MediaType mediaType = MediaType.parse("application/vnd.api+json");
            return RequestBody.create(mediaType, converter.writeObject(value));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
