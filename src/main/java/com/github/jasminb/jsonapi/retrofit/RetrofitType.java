package com.github.jasminb.jsonapi.retrofit;

import com.github.jasminb.jsonapi.JSONAPIDocument;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Simple class used to simplify type management in Retrofit converter factory.
 */
public class RetrofitType {
    private Class<?> type;

    private boolean collection;
    private boolean valid = true;
    private boolean isParentType = false;

    /**
     * Instantiates a new Retrofit type.
     *
     * @param type the type
     */
    public RetrofitType(Type type) {
        if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(JSONAPIDocument.class)) {
            initialize(((ParameterizedType) type).getActualTypeArguments()[0]);
            isParentType = true;
        } else {
            initialize(type);
        }
    }

    private void initialize(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
            if (typeArgs != null && typeArgs.length > 0) {
                this.type = (Class<?>) typeArgs[0];
                this.collection = true;
            } else {
                valid = false;
            }
        } else if (type instanceof Class) {
            this.type = (Class<?>) type;
        } else {
            valid = false;
        }
    }

	/**
	 * Returns true if type used to instantiate this object was wrapped with {@link JSONAPIDocument}.
	 * @return {@link Boolean}
	 */
	public boolean isJSONAPIDocumentType() {
        return isParentType;
    }

    /**
     * Gets type.
     *
     * @return the type
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Is collection boolean.
     *
     * @return <code>true</code> if type was ParameterizedType else <code>false</code>
     */
    public boolean isCollection() {
        return collection;
    }

    /**
     * Is valid boolean.
     *
     * @return <code>true</code> if type is valid, else <code>false</code>
     */
    public boolean isValid() {
        return valid;
    }
}
