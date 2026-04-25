package com.javatreesearch.lombok;

// Feature: lombok-reference-support, Property 5: Derivação de getter para campos booleanos

import net.jqwik.api.*;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for LombokMethodNameDeriver boolean getter derivation.
 *
 * **Validates: Requirements 2.2**
 */
class LombokMethodNameDeriverBooleanPropertiesTest {

    private final LombokMethodNameDeriver deriver = new LombokMethodNameDeriverImpl();

    /**
     * Property 5: Derivação de getter para campos booleanos (tipo boolean primitivo, @Getter)
     *
     * For any valid Java field name of type boolean with @Getter active,
     * the derived getter name SHALL be equal to "is" + capitalize(fieldName).
     *
     * Validates: Requirements 2.2
     */
    @Property(tries = 100)
    void booleanPrimitiveGetterWithGetterAnnotation(
            @ForAll("validJavaFieldNames") String fieldName
    ) {
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.GETTER);
        Set<String> existingMethods = Collections.emptySet();

        Set<String> derived = deriver.deriveMethodNames(fieldName, "boolean", annotations, existingMethods);

        String expectedGetter = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        assertTrue(derived.contains(expectedGetter),
                "Derived methods should contain boolean getter '" + expectedGetter + "' for field '" + fieldName
                        + "' of type 'boolean', but got: " + derived);

        // Boolean fields use "is" prefix, not "get"
        String unexpectedGetter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        assertFalse(derived.contains(unexpectedGetter),
                "Derived methods should NOT contain 'get' getter '" + unexpectedGetter + "' for boolean field '"
                        + fieldName + "', but got: " + derived);
    }

    /**
     * Property 5 (variant): Same property holds for Boolean wrapper type with @Getter.
     *
     * Validates: Requirements 2.2
     */
    @Property(tries = 100)
    void booleanWrapperGetterWithGetterAnnotation(
            @ForAll("validJavaFieldNames") String fieldName
    ) {
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.GETTER);
        Set<String> existingMethods = Collections.emptySet();

        Set<String> derived = deriver.deriveMethodNames(fieldName, "Boolean", annotations, existingMethods);

        String expectedGetter = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        assertTrue(derived.contains(expectedGetter),
                "Derived methods should contain boolean getter '" + expectedGetter + "' for field '" + fieldName
                        + "' of type 'Boolean', but got: " + derived);

        // Boolean fields use "is" prefix, not "get"
        String unexpectedGetter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        assertFalse(derived.contains(unexpectedGetter),
                "Derived methods should NOT contain 'get' getter '" + unexpectedGetter + "' for Boolean field '"
                        + fieldName + "', but got: " + derived);
    }

    /**
     * Property 5 (variant): Same property holds for boolean primitive type with @Data.
     *
     * Validates: Requirements 2.2
     */
    @Property(tries = 100)
    void booleanPrimitiveGetterWithDataAnnotation(
            @ForAll("validJavaFieldNames") String fieldName
    ) {
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.DATA);
        Set<String> existingMethods = Collections.emptySet();

        Set<String> derived = deriver.deriveMethodNames(fieldName, "boolean", annotations, existingMethods);

        String expectedGetter = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        assertTrue(derived.contains(expectedGetter),
                "Derived methods should contain boolean getter '" + expectedGetter + "' for field '" + fieldName
                        + "' of type 'boolean' with @Data, but got: " + derived);

        // Boolean fields use "is" prefix, not "get"
        String unexpectedGetter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        assertFalse(derived.contains(unexpectedGetter),
                "Derived methods should NOT contain 'get' getter '" + unexpectedGetter + "' for boolean field '"
                        + fieldName + "' with @Data, but got: " + derived);
    }

    /**
     * Property 5 (variant): Same property holds for Boolean wrapper type with @Data.
     *
     * Validates: Requirements 2.2
     */
    @Property(tries = 100)
    void booleanWrapperGetterWithDataAnnotation(
            @ForAll("validJavaFieldNames") String fieldName
    ) {
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.DATA);
        Set<String> existingMethods = Collections.emptySet();

        Set<String> derived = deriver.deriveMethodNames(fieldName, "Boolean", annotations, existingMethods);

        String expectedGetter = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        assertTrue(derived.contains(expectedGetter),
                "Derived methods should contain boolean getter '" + expectedGetter + "' for field '" + fieldName
                        + "' of type 'Boolean' with @Data, but got: " + derived);

        // Boolean fields use "is" prefix, not "get"
        String unexpectedGetter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        assertFalse(derived.contains(unexpectedGetter),
                "Derived methods should NOT contain 'get' getter '" + unexpectedGetter + "' for Boolean field '"
                        + fieldName + "' with @Data, but got: " + derived);
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
}
