package com.github.jasminb.jsonapi.retrofit;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Simple class used to simplify type management in Retrofit converter factory.
 */
public class RetrofitType {
	private Class<?> type;
	private boolean collection;
	private boolean valid = true;

	/**
	 * Instantiates a new Retrofit type.
	 *
	 * @param type the type
	 */
	public RetrofitType(Type type) {
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
