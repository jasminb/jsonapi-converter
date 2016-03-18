package com.github.jasminb.jsonapi;

import com.github.jasminb.jsonapi.annotations.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
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
	 * Returns the type name defined using Type annotation on provided class.
	 * @param clazz type class
	 * @return name of the type or <code>null</code> in case Type annotation is not present
	 */
	public static String getTypeName(Class<?> clazz) {
		Type typeAnnotation = clazz.getAnnotation(Type.class);
		return typeAnnotation != null ? typeAnnotation.value() : null;
	}

	public static Class<?> getFieldType(Field field) {
		Class<?> targetType = field.getType();

		if (targetType.equals(List.class)) {
			ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
			targetType = (Class<?>) stringListType.getActualTypeArguments()[0];
		}

		return targetType;
	}
}
