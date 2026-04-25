package com.javatreesearch.lombok;

// Feature: lombok-reference-support, Property 7: Derivação de método builder e construtor

import net.jqwik.api.*;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for LombokMethodNameDeriver builder method and constructor derivation.
 *
 * **Validates: Requirements 2.4, 2.5**
 */
class LombokMethodNameDeriverBuilderPropertiesTest {

    private final LombokMethodNameDeriver deriver = new LombokMethodNameDeriverImpl();

    /**
     * Property 7: Derivação de método builder e construtor (builder part)
     *
     * For any valid Java field name with @Builder active, the derived builder method name
     * SHALL be equal to the fieldName itself (no prefix).
     *
     * Validates: Requirements 2.4
     */
    @Property(tries = 100)
    void builderMethodNameEqualsFieldName(
            @ForAll("validJavaFieldNames") String fieldName
    ) {
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.BUILDER);
        Set<String> existingMethods = Collections.emptySet();

        Set<String> derived = deriver.deriveMethodNames(fieldName, "String", annotations, existingMethods);

        assertTrue(derived.contains(fieldName),
                "Derived methods should contain builder method '" + fieldName
                        + "' (no prefix) for field '" + fieldName
                        + "' with @Builder, but got: " + derived);
    }

    /**
     * Property 7: Derivação de método builder e construtor (AllArgsConstructor part)
     *
     * For any class name with @AllArgsConstructor, the derived constructor name
     * SHALL be equal to the class name.
     *
     * Validates: Requirements 2.5
     */
    @Property(tries = 100)
    void allArgsConstructorNameEqualsClassName(
            @ForAll("validJavaClassNames") String className
    ) {
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.ALL_ARGS_CONSTRUCTOR);

        Set<String> derived = deriver.deriveConstructorNames(className, annotations);

        assertTrue(derived.contains(className),
                "Derived constructor names should contain class name '" + className
                        + "' with @AllArgsConstructor, but got: " + derived);
    }

    /**
     * Property 7: Derivação de método builder e construtor (RequiredArgsConstructor part)
     *
     * For any class name with @RequiredArgsConstructor, the derived constructor name
     * SHALL be equal to the class name.
     *
     * Validates: Requirements 2.5
     */
    @Property(tries = 100)
    void requiredArgsConstructorNameEqualsClassName(
            @ForAll("validJavaClassNames") String className
    ) {
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.REQUIRED_ARGS_CONSTRUCTOR);

        Set<String> derived = deriver.deriveConstructorNames(className, annotations);

        assertTrue(derived.contains(className),
                "Derived constructor names should contain class name '" + className
                        + "' with @RequiredArgsConstructor, but got: " + derived);
    }

    /**
     * Generates valid Java field names: lowercase letter start, followed by lowercase letters only.
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
     * Generates valid Java class names: uppercase letter start, followed by lowercase letters only.
     */
    @Provide
    Arbitrary<String> validJavaClassNames() {
        Arbitrary<Character> firstChar = Arbitraries.chars().range('A', 'Z');
        Arbitrary<String> rest = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(0)
                .ofMaxLength(19);
        return Combinators.combine(firstChar, rest)
                .as((first, tail) -> first + tail);
    }
}
