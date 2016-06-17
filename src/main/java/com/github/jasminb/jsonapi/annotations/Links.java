package com.github.jasminb.jsonapi.annotations;

import java.lang.annotation.*;

/**
 * Annotation used to configure links field in JSON API resources.
 *
 * @author jbegic
 */

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Links {
}
