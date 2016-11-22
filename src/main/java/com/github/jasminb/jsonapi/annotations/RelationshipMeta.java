package com.github.jasminb.jsonapi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Relationship meta annotation.
 *
 * This annotation is used in conjunction with @Relationship annotation, attribute annotated with this annotation
 * is considered to be meta-data related to named relationship object.
 *
 * <pre>
 * <code>
 *     {@literal @}Type("my-type")
 *     public class MyType {
 *     	   {@literal @}Id
 *         private String id;
 *
 *         {@literal @}Relationship("relationship")
 *         private MyRelationship myRelationship;
 *
 *         {@literal @}RelationshipMeta("relationship")
 *         private MyRelationshipMeta myRelationshipMeta;
 *
 *     }
 * </code>
 * </pre>
 *
 * @author jbegic
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RelationshipMeta {
	String value();
}
