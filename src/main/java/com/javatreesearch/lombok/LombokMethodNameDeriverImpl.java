package com.javatreesearch.lombok;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Implementation of {@link LombokMethodNameDeriver} that derives method names
 * Lombok would generate for annotated fields and classes.
 *
 * <p>Derivation rules:
 * <ul>
 *   <li>{@code @Getter} or {@code @Data} present, non-boolean type → {@code "get" + capitalize(fieldName)}</li>
 *   <li>{@code @Getter} or {@code @Data} present, type {@code boolean} or {@code Boolean} → {@code "is" + capitalize(fieldName)}</li>
 *   <li>{@code @Setter} or {@code @Data} present → {@code "set" + capitalize(fieldName)}</li>
 *   <li>{@code @Builder} present on class → {@code fieldName} (no prefix)</li>
 *   <li>{@code @AllArgsConstructor} or {@code @RequiredArgsConstructor} present → class name</li>
 *   <li>Derived name already exists in {@code existingMethods} → omitted (no duplicate)</li>
 * </ul>
 */
public class LombokMethodNameDeriverImpl implements LombokMethodNameDeriver {

    @Override
    public Set<String> deriveMethodNames(
            String fieldName,
            String fieldType,
            Set<LombokAnnotation> annotations,
            Set<String> existingMethods) {

        Set<String> derived = new LinkedHashSet<>();

        boolean hasGetter = annotations.contains(LombokAnnotation.GETTER)
                || annotations.contains(LombokAnnotation.DATA);
        boolean hasSetter = annotations.contains(LombokAnnotation.SETTER)
                || annotations.contains(LombokAnnotation.DATA);
        boolean hasBuilder = annotations.contains(LombokAnnotation.BUILDER);

        if (hasGetter) {
            String getterName = isBoolean(fieldType)
                    ? "is" + capitalize(fieldName)
                    : "get" + capitalize(fieldName);
            if (!existingMethods.contains(getterName)) {
                derived.add(getterName);
            }
        }

        if (hasSetter) {
            String setterName = "set" + capitalize(fieldName);
            if (!existingMethods.contains(setterName)) {
                derived.add(setterName);
            }
        }

        if (hasBuilder) {
            String builderName = fieldName;
            if (!existingMethods.contains(builderName)) {
                derived.add(builderName);
            }
        }

        return derived;
    }

    @Override
    public Set<String> deriveConstructorNames(String className, Set<LombokAnnotation> annotations) {
        Set<String> derived = new LinkedHashSet<>();

        boolean hasConstructorAnnotation =
                annotations.contains(LombokAnnotation.ALL_ARGS_CONSTRUCTOR)
                || annotations.contains(LombokAnnotation.REQUIRED_ARGS_CONSTRUCTOR);

        if (hasConstructorAnnotation) {
            derived.add(className);
        }

        return derived;
    }

    /**
     * Returns {@code true} if the given type name represents a boolean type
     * ({@code boolean} primitive or {@code Boolean} wrapper).
     */
    private static boolean isBoolean(String fieldType) {
        return "boolean".equals(fieldType) || "Boolean".equals(fieldType);
    }

    /**
     * Capitalizes the first character of the given name, leaving the rest unchanged.
     * Returns the original string if it is null or empty.
     */
    static String capitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
