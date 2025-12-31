package com.github.jasminb.jsonapi.abstraction;

/**
 * Strategy for translating Java field names to JSON property names.
 *
 * Epic 5.5: Complete Jackson Abstraction - Field naming abstraction
 */
public interface FieldNamingStrategy {

    /**
     * Translates a Java field name to a JSON property name.
     *
     * @param fieldName the Java field name
     * @return the JSON property name
     */
    String translateName(String fieldName);

    // ===== COMMON STRATEGIES =====

    /**
     * Identity strategy - returns field name unchanged.
     */
    FieldNamingStrategy IDENTITY = fieldName -> fieldName;

    /**
     * Snake case strategy - converts camelCase to snake_case.
     * Example: "firstName" -> "first_name"
     */
    FieldNamingStrategy SNAKE_CASE = fieldName -> {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    };

    /**
     * Kebab case strategy - converts camelCase to kebab-case.
     * Example: "firstName" -> "first-name"
     */
    FieldNamingStrategy KEBAB_CASE = fieldName -> {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('-');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    };

    /**
     * Lower case strategy - converts to all lowercase.
     * Example: "FirstName" -> "firstname"
     */
    FieldNamingStrategy LOWER_CASE = fieldName ->
        fieldName != null ? fieldName.toLowerCase() : null;

    /**
     * Upper case strategy - converts to all uppercase.
     * Example: "firstName" -> "FIRSTNAME"
     */
    FieldNamingStrategy UPPER_CASE = fieldName ->
        fieldName != null ? fieldName.toUpperCase() : null;
}
