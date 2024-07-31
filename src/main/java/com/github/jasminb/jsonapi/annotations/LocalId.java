package com.github.jasminb.jsonapi.annotations;

import com.github.jasminb.jsonapi.ResourceIdHandler;
import com.github.jasminb.jsonapi.StringIdHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark resource field as a local identifier (<code>lid</code>).
 *
 * @author jbegic
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LocalId {
	Class<? extends ResourceIdHandler> value() default StringIdHandler.class;
}
