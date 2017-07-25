package com.github.jasminb.jsonapi;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Meta;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.RelationshipLinks;
import com.github.jasminb.jsonapi.annotations.RelationshipMeta;
import com.github.jasminb.jsonapi.annotations.Type;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converter configuration.
 *
 * <p>
 *     This class exposes contracts needed to handle JSON API spec serialization/deserialization.
 * </p>
 *
 * @author jbegic
 */
public class ConverterConfiguration {

	private final Map<String, Class<?>> typeToClassMapping = new HashMap<>();
	private final Map<Class<?>, Type> typeAnnotations = new HashMap<>();
	private final Map<Class<?>, Field> idMap = new HashMap<>();
	private final Map<Class<?>, ResourceIdHandler> idHandlerMap = new HashMap<>();
	private final Map<Class<?>, List<Field>> relationshipMap = new HashMap<>();
	private final Map<Class<?>, Map<String, Class<?>>> relationshipTypeMap = new HashMap<>();
	private final Map<Class<?>, Map<String, Field>> relationshipFieldMap = new HashMap<>();
	private final Map<Field, Relationship> fieldRelationshipMap = new HashMap<>();
	private final Map<Field, RelationshipMeta> fieldRelationshipMetaMap = new HashMap<>();
	private final Map<Class<?>, Map<String, Class<?>>> relationshipMetaTypeMap = new HashMap<>();
	private final Map<Class<?>, Map<String, Field>> relationshipMetaFieldMap = new HashMap<>();
	private final Map<Class<?>, Class<?>> metaTypeMap = new HashMap<>();
	private final Map<Class<?>, Field> metaFieldMap = new HashMap<>();
	private final Map<Class<?>, Field> linkFieldMap = new HashMap<>();
	
	// Relationship links lookups
	private final Map<Class<?>, Map<String, Field>> relationshipLinksFieldMap = new HashMap<>();

	/**
	 * Creates new ConverterConfiguration.
	 * @param classes list of mapped classes
	 */
	public ConverterConfiguration(Class<?>... classes) {
		for (Class<?> clazz : classes) {
			registerType(clazz);
		}
	}

	private void processClass(Class<?> clazz) {
		if (clazz.isAnnotationPresent(Type.class)) {
			Type annotation = clazz.getAnnotation(Type.class);
			typeToClassMapping.put(annotation.value(), clazz);
			typeAnnotations.put(clazz, annotation);
			relationshipTypeMap.put(clazz, new HashMap<String, Class<?>>());
			relationshipFieldMap.put(clazz, new HashMap<String, Field>());
			relationshipMetaFieldMap.put(clazz, new HashMap<String, Field>());
			relationshipMetaTypeMap.put(clazz, new HashMap<String, Class<?>>());
			relationshipLinksFieldMap.put(clazz, new HashMap<String, Field>());

			// collecting Relationship fields
			List<Field> relationshipFields = ReflectionUtils.getAnnotatedFields(clazz, Relationship.class, true);

			for (Field relationshipField : relationshipFields) {
				relationshipField.setAccessible(true);

				Relationship relationship = relationshipField.getAnnotation(Relationship.class);
				Class<?> targetType = ReflectionUtils.getFieldType(relationshipField);
				relationshipTypeMap.get(clazz).put(relationship.value(), targetType);
				relationshipFieldMap.get(clazz).put(relationship.value(), relationshipField);
				fieldRelationshipMap.put(relationshipField, relationship);

				if (relationship.resolve() && relationship.relType() == null) {
					throw new IllegalArgumentException("@Relationship on " + clazz.getName() + "#" +
							relationshipField.getName() + " with 'resolve = true' must have a relType attribute " +
							"set." );
				}

				registerType(targetType);
			}

			relationshipMap.put(clazz, relationshipFields);
			
			// collecting RelationshipMeta fields
			List<Field> relMetaFields = ReflectionUtils.getAnnotatedFields(clazz, RelationshipMeta.class, true);
			
			for (Field relMetaField : relMetaFields) {
				relMetaField.setAccessible(true);
				
				RelationshipMeta relationshipMeta = relMetaField.getAnnotation(RelationshipMeta.class);
				Class<?> targetType = ReflectionUtils.getFieldType(relMetaField);
				relationshipMetaTypeMap.get(clazz).put(relationshipMeta.value(), targetType);
				fieldRelationshipMetaMap.put(relMetaField, relationshipMeta);
				relationshipMetaFieldMap.get(clazz).put(relationshipMeta.value(), relMetaField);
			}
			
			// Collecting RelationshipLink fields
			List<Field> relLinkFields = ReflectionUtils.getAnnotatedFields(clazz, RelationshipLinks.class, true);
			
			for (Field relLinkField : relLinkFields) {
				relLinkField.setAccessible(true);
				
				RelationshipLinks links = relLinkField.getAnnotation(RelationshipLinks.class);
				relationshipLinksFieldMap.get(clazz).put(links.value(), relLinkField);
			}

			// collecting Id fields
			List<Field> idAnnotatedFields = ReflectionUtils.getAnnotatedFields(clazz, Id.class, true);

			if (!idAnnotatedFields.isEmpty() && idAnnotatedFields.size() == 1) {
				Field idField = idAnnotatedFields.get(0);
				idField.setAccessible(true);
				idMap.put(clazz, idField);
				try {
					idHandlerMap.put(clazz, idField.getAnnotation(Id.class).value().newInstance());
				} catch (InstantiationException | IllegalAccessException e) {
					throw new IllegalArgumentException("Unable to construct handler instance by using no-arg constructor", e);
				}
			} else {
				if (idAnnotatedFields.isEmpty()) {
					throw new IllegalArgumentException("All resource classes must have a field annotated with the " +
							"@Id annotation");
				} else {
					throw new IllegalArgumentException("Only single @Id annotation is allowed per defined type!");
				}

			}

			// Collecting Meta fields
			List<Field> metaFields = ReflectionUtils.getAnnotatedFields(clazz, Meta.class, true);
			if (metaFields.size() == 1) {
				Field metaField = metaFields.get(0);
				metaField.setAccessible(true);
				Class<?> metaType = ReflectionUtils.getFieldType(metaField);
				metaTypeMap.put(clazz, metaType);
				metaFieldMap.put(clazz, metaField);
			} else if (metaFields.size() > 1) {
				throw new IllegalArgumentException(String.format("Only one meta field is allowed for type '%s'",
						clazz.getCanonicalName()));
			}

			// Collect and handle 'Link' field
			List<Field> linkFields = ReflectionUtils.getAnnotatedFields(clazz,
					com.github.jasminb.jsonapi.annotations.Links.class, true);

			if (linkFields.size() == 1) {
				Field linkField = linkFields.get(0);
				linkField.setAccessible(true);

				Class<?> metaType = ReflectionUtils.getFieldType(linkField);

				if (!Links.class.isAssignableFrom(metaType)) {
					throw new IllegalArgumentException(String.format("%s is not allowed to be used as @Links " +
							"attribute. Only com.github.jasminb.jsonapi.Links or its derivatives" +
							" can be annotated as @Links", metaType.getCanonicalName()));
				} else {
					linkFieldMap.put(clazz, linkField);
				}
			} else if (linkFields.size() > 1) {
				throw new IllegalArgumentException(String.format("Only one links field is allowed for type '%s'",
						clazz.getCanonicalName()));
			}

		} else {
			if (!clazz.isInterface()) {
				throw new IllegalArgumentException("Class " + clazz.getName() +
						" don't have Type annotation. All resource classes must be annotated with Type annotation!");
			}
		}
	}

	/**
	 * Returns field annotated with meta annotation for given type.
	 * @param clazz {@link Class} type
	 * @return {@link Field} meta field or <code>null</code>
	 */
	public Field getMetaField(Class<?> clazz) {
		return metaFieldMap.get(clazz);
	}

	/**
	 * Returns meta-data type for given class.
	 * @param clazz {@link Class} class
	 * @return {@link Class} type or <code>null</code> if no field with meta annotaiton is found on given type
	 */
	public Class<?> getMetaType(Class<?> clazz) {
		return metaTypeMap.get(clazz);
	}

	/**
	 * Resolves link field for given type.
	 * @param clazz {@link Class} type
	 * @return {@link Field} or <code>null</code>
	 */
	public Field getLinksField(Class<?> clazz) {
		return linkFieldMap.get(clazz);
	}

	/**
	 * Resolves a type for given type name.
	 * @param typeName {@link String} type name
	 * @return {@link Class} resolved type
	 */
	public Class<?> getTypeClass(String typeName) {
		return typeToClassMapping.get(typeName);
	}

	/**
	 * Returns the id field for given type.
	 * @param clazz {@link Class} type to resolve id field for
	 * @return {@link Field} id field
	 */
	public Field getIdField(Class<?> clazz) {
		return idMap.get(clazz);
	}
	
	/**
	 * Returns handler registered for given type's id field.
	 *
	 * @param clazz {@link Class} type to resolve id handler for
	 * @return handler
	 */
	public ResourceIdHandler getIdHandler(Class<?> clazz) {
		return idHandlerMap.get(clazz);
	}

	/**
	 * Returns relationship field.
	 * @param clazz {@link Class} class holding relationship
	 * @param fieldName {@link String} name of the field
	 * @return {@link Field} field
	 */
	public Field getRelationshipField(Class<?> clazz, String fieldName) {
		return relationshipFieldMap.get(clazz).get(fieldName);
	}

	/**
	 * Returns a type of a relationship.
	 * @param clazz {@link Class} owning the field with relationship annotation
	 * @param fieldName {@link String} name of the field
	 * @return {@link Class} field type
	 */
	public Class<?> getRelationshipType(Class<?> clazz, String fieldName) {
		return relationshipTypeMap.get(clazz).get(fieldName);
	}

	/**
	 * Resolves {@link Relationship} instance for given field.
	 *
	 * <p>
	 *     This works for fields that were found to be annotated with {@link Relationship} annotation.
	 * </p>
	 * @param field {@link Field} to get relationship for
	 * @return {@link Relationship} anotation or <code>null</code>
	 */
	public Relationship getFieldRelationship(Field field) {
		return fieldRelationshipMap.get(field);
	}

	/**
	 * Returns list of all fields annotated with {@link Relationship} annotation for given class.
	 * @param clazz {@link Class} to get relationship fields for
	 * @return list of relationship fields
	 */
	public List<Field> getRelationshipFields(Class<?> clazz) {
		return relationshipMap.get(clazz);
	}

	/**
	 * Checks if provided class was registered with this configuration instance.
	 * @param clazz {@link Class} to check
	 * @return <code>true</code> if class was registed else <code>false</code>
	 */
	public boolean isRegisteredType(Class<?> clazz) {
		return typeAnnotations.containsKey(clazz);
	}

	/**
	 * Resolves and returns name of the type given to provided class.
	 * @param clazz {@link Class} to resolve type name for
	 * @return type name or <code>null</code> if type was not registered
	 */
	public String getTypeName(Class<?> clazz) {
		Type type = typeAnnotations.get(clazz);

		if (type != null) {
			return type.value();
		}
		return null;
	}
	
	/**
	 * Resolves and returns the type given to provided class.
	 * @param clazz {@link Class} to resolve type name for
	 * @return type or <code>null</code> if type was not registered
	 */
	public Type getType(Class<?> clazz) {
		return typeAnnotations.get(clazz);
	}

	/**
	 * Registers new type with this configuration instance.
	 * @param type {@link Class} type to register
	 * @return <code>true</code> in case type was registered, <code>false</code> in case type was registered already
	 */
	public synchronized boolean registerType(Class<?> type) {
		if (!isRegisteredType(type)) {
			processClass(type);
			return true;
		}

		return false;
	}

	/**
	 * Checks if class is eligible as resource type (must have {@link Type} annotation and field annotated with
	 * {@link Id}.
	 * @param type type to test
	 * @return <code>true</code> if type is eligible
	 */
	public static boolean isEligibleType(Class<?> type) {
		return type.isAnnotationPresent(Type.class) &&
				!ReflectionUtils.getAnnotatedFields(type, Id.class, true).isEmpty();
	}
	
	/**
	 * Returns relationship meta field.
	 * @param clazz {@link Class} class holding relationship
	 * @param relationshipName {@link String} name of the relationship
	 * @return {@link Field} field
	 */
	public Field getRelationshipMetaField(Class<?> clazz, String relationshipName) {
		return relationshipMetaFieldMap.get(clazz).get(relationshipName);
	}
	
	/**
	 * Returns a type of a relationship meta field.
	 * @param clazz {@link Class} owning the field with relationship meta annotation
	 * @param relationshipName {@link String} name of the field
	 * @return {@link Class} meta field type
	 */
	public Class<?> getRelationshipMetaType(Class<?> clazz, String relationshipName) {
		return relationshipMetaTypeMap.get(clazz).get(relationshipName);
	}
	
	/**
	 * Returns relationship links field.
	 * @param clazz {@link Class} class holding relationship
	 * @param relationshipName {@link String} name of the relationship
	 * @return {@link Field} field
	 */
	public Field getRelationshipLinksField(Class<?> clazz, String relationshipName) {
		return relationshipLinksFieldMap.get(clazz).get(relationshipName);
	}

}
