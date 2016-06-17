package com.github.jasminb.jsonapi.annotations;

import com.github.jasminb.jsonapi.RelType;

import java.lang.annotation.*;

/**
 * Annotation used to configure relationship field in JSON API resources.
 *
 * @author jbegic
 */

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Relationship {
	String value();
	boolean resolve() default false;
	boolean serialise() default true;
	RelType relType() default RelType.SELF;
}
