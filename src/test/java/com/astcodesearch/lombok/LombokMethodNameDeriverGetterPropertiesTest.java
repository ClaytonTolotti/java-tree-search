package com.javatreesearch.lombok;

// Feature: lombok-reference-support, Property 4: Derivação de getter para campos não-booleanos

import net.jqwik.api.*;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for LombokMethodNameDeriver non-boolean getter derivation.
 *
 * **Validates: Requirements 2.1, 2.7**
 */
class LombokMethodNameDeriverGetterPropertiesTest {

    private final LombokMethodNameDeriver deriver = new LombokMethodNameDeriverImpl();

    /**
     * Property 4: Derivação de getter para campos não-booleanos
     *
     * For any valid Java field name of non-boolean type with @Getter or @Data active,
     * the derived getter name SHALL be equal to "get" + capitalize(fieldName).
     *
     * Validates: Requirements 2.1, 2.7
     */
    @Property(tries = 100)
    void nonBooleanGetterWithGetterAnnotation(
            @ForAll("validJavaFieldNames") String fieldName,
            @ForAll("nonBooleanTypes") String fieldType
    ) {
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.GETTER);
        Set<String> existingMethods = Collections.emptySet();

        Set<String> derived = deriver.deriveMethodNames(fieldName, fieldType, annotations, existingMethods);

        String expectedGetter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        assertTrue(derived.contains(expectedGetter),
                "Derived methods should contain getter '" + expectedGetter + "' for field '" + fieldName
                        + "' of type '" + fieldType + "', but got: " + derived);

        // Also assert the derived name is a valid Java identifier
        assertTrue(Character.isLetter(expectedGetter.charAt(0)),
                "Getter name must start with a letter, but was: " + expectedGetter);
        assertTrue(expectedGetter.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_'),
                "Getter name must contain only letters, digits, or underscores, but was: " + expectedGetter);
    }

    /**
     * Property 4 (variant): Same property holds when @Data is the active annotation instead of @Getter.
     *
     * Validates: Requirements 2.1, 2.7
     */
    @Property(tries = 100)
    void nonBooleanGetterWithDataAnnotation(
            @ForAll("validJavaFieldNames") String fieldName,
            @ForAll("nonBooleanTypes") String fieldType
    ) {
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.DATA);
        Set<String> existingMethods = Collections.emptySet();

        Set<String> derived = deriver.deriveMethodNames(fieldName, fieldType, annotations, existingMethods);

        String expectedGetter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        assertTrue(derived.contains(expectedGetter),
                "Derived methods should contain getter '" + expectedGetter + "' for field '" + fieldName
                        + "' of type '" + fieldType + "' with @Data, but got: " + derived);

        // Also assert the derived name is a valid Java identifier
        assertTrue(Character.isLetter(expectedGetter.charAt(0)),
                "Getter name must start with a letter, but was: " + expectedGetter);
        assertTrue(expectedGetter.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_'),
                "Getter name must contain only letters, digits, or underscores, but was: " + expectedGetter);
    }

    /**
     * Generates valid Java field names: lowercase letter start, followed by lowercase letters only.
     * This ensures the generated names are valid Java identifiers.
     */
    @Provide
    Arbitrary<String> validJavaFieldNames() {
        Arbitrary<Character> firstChar = Arbitraries.chars().range('a', 'z');
        Arbitrary<String> rest = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(0)
                .ofMaxLength(19);
        return Combinators.combine(firstChar, rest)
                .as((first, tail) -> first + tail);
    }

    /**
     * Generates non-boolean type names.
     */
    @Provide
    Arbitrary<String> nonBooleanTypes() {
        return Arbitraries.of("String", "int", "long", "double", "Object", "Integer", "Long", "Double", "List", "Map");
    }
}
