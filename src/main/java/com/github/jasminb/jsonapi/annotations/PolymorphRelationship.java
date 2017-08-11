package com.github.jasminb.jsonapi.annotations;

import com.github.jasminb.jsonapi.RelType;

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
public @interface PolymorphRelationship {
	String value();
	boolean resolve() default false;
	boolean serialise() default true;
	RelType relType() default RelType.SELF;
	
	/**
	 * Resource path, used to generate <code>self</code> link.
	 */
	String path() default "";
	
	/**
	 * Resource path, used to generate <code>related</code> link.
	 */
	String relatedPath() default "";
}
