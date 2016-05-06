package com.github.jasminb.jsonapi.annotations;

import com.github.jasminb.jsonapi.RelType;
import com.github.jasminb.jsonapi.ResolutionStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
	ResolutionStrategy strategy() default ResolutionStrategy.OBJECT;
}
