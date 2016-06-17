package com.github.jasminb.jsonapi.annotations;

import java.lang.annotation.*;

/**
 * Annotation used to configure meta field in JSON API resources.
 *
 * @author jbegic
 */

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Meta {
}
