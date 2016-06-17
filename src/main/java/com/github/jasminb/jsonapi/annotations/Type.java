package com.github.jasminb.jsonapi.annotations;

import java.lang.annotation.*;

/**
 * Used for declaring type as a JSON API resource.
 *
 * @author jbegic
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Type {
	String value();
}
