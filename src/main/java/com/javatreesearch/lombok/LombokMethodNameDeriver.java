package com.javatreesearch.lombok;

import java.util.Set;

public interface LombokMethodNameDeriver {
    /**
     * Derives the set of method names Lombok would generate for a field.
     *
     * @param fieldName       the field name (e.g., "pathDownload")
     * @param fieldType       the field type simple name (e.g., "String", "boolean")
     * @param annotations     active Lombok annotations (class-level + field-level)
     * @param existingMethods method names already declared in source (to avoid duplicates)
     * @return set of derived method names (e.g., {"getPathDownload", "setPathDownload"})
     */
    Set<String> deriveMethodNames(
        String fieldName,
        String fieldType,
        Set<LombokAnnotation> annotations,
        Set<String> existingMethods
    );

    /**
     * Derives the constructor method name for a class (used for @AllArgsConstructor etc.).
     *
     * @param className   the simple class name
     * @param annotations active Lombok annotations on the class
     * @return set containing the class name if a constructor annotation is present, else empty
     */
    Set<String> deriveConstructorNames(String className, Set<LombokAnnotation> annotations);
}
