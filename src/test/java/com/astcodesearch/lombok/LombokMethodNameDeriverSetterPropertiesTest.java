package com.javatreesearch.lombok;

// Feature: lombok-reference-support, Property 6: Derivação de setter

import net.jqwik.api.*;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for LombokMethodNameDeriver setter derivation.
 *
 * **Validates: Requirements 2.3**
 */
class LombokMethodNameDeriverSetterPropertiesTest {

    private final LombokMethodNameDeriver deriver = new LombokMethodNameDeriverImpl();

    /**
     * Property 6: Derivação de setter
     *
     * For any valid Java field name with @Setter active, the derived setter name
     * SHALL be equal to "set" + capitalize(fieldName).
     *
     * Validates: Requirements 2.3
     */
    @Property(tries = 100)
    void setterWithSetterAnnotation(
            @ForAll("validJavaFieldNames") String fieldName
    ) {
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.SETTER);
        Set<String> existingMethods = Collections.emptySet();

        Set<String> derived = deriver.deriveMethodNames(fieldName, "String", annotations, existingMethods);

        String expectedSetter = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        assertTrue(derived.contains(expectedSetter),
                "Derived methods should contain setter '" + expectedSetter + "' for field '" + fieldName
                        + "' with @Setter, but got: " + derived);
    }

    /**
     * Property 6 (variant): Same property holds when @Data is the active annotation instead of @Setter.
     *
     * Validates: Requirements 2.3
     */
    @Property(tries = 100)
    void setterWithDataAnnotation(
            @ForAll("validJavaFieldNames") String fieldName
    ) {
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.DATA);
        Set<String> existingMethods = Collections.emptySet();

        Set<String> derived = deriver.deriveMethodNames(fieldName, "String", annotations, existingMethods);

        String expectedSetter = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        assertTrue(derived.contains(expectedSetter),
                "Derived methods should contain setter '" + expectedSetter + "' for field '" + fieldName
                        + "' with @Data, but got: " + derived);
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
