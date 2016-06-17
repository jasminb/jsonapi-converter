package com.github.jasminb.jsonapi.annotations;

import java.lang.annotation.*;

/**
 * Annotation used to mark resource field as an id in JSON API resource class.
 *
 * @author jbegic
 */

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Id {
}
