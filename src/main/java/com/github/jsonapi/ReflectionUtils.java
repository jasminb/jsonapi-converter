package com.github.jsonapi;

import com.github.jsonapi.annotations.Relationship;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Various utility methods that simplify JAVA reflection actions.
 *
 * @author jbegic
 */
public class ReflectionUtils {

	private ReflectionUtils() {
		// Private CTOR
	}

	/**
	 * Returns all field from a given class that are annotated with provided annotation type.
	 * @param clazz source class
	 * @param annotation target annotation
	 * @return list of fields or empty collection in case no fields were found
	 */
	public static List<Field> getAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation) {
		Field [] fields = clazz.getDeclaredFields();

		List<Field> result = new ArrayList<>();

		for (Field field : fields) {
			if (field.isAnnotationPresent(annotation)) {
				result.add(field);
			}
		}

		return result;
	}


	/**
	 * Returns a field that has annotation with its relationship name equal to provided name.
	 * @param clazz source class
	 * @param relationshipName name of the resource relationship
	 * @return field or null if it was not found
	 */
	public static Field getRelationshipField(Class<?> clazz, String relationshipName) {
		List<Field> annotatedFields = getAnnotatedFields(clazz, Relationship.class);

		for (Field annotatedField : annotatedFields) {
			Relationship fieldRelationship = annotatedField.getAnnotation(Relationship.class);

			String annotationName = fieldRelationship.name();
			if (annotationName.isEmpty()) {
				annotationName = annotatedField.getName();
			}

			if (annotationName.equals(relationshipName)) {
				return annotatedField;
			}
		}

		return null;
	}
}
