package com.github.jasminb.jsonapi.annotations;

import com.github.jasminb.jsonapi.ResourceIdHandler;
import com.github.jasminb.jsonapi.StringIdHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark resource field as an id in JSON API resource class.
 *
 * @author jbegic
 */

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
	Class<? extends ResourceIdHandler> value() default StringIdHandler.class;
}
