package com.javatreesearch.lombok;

// Feature: lombok-reference-support, Property 8: Sem duplicatas quando getter explícito existe

import net.jqwik.api.*;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for LombokMethodNameDeriver deduplication behavior.
 *
 * **Validates: Requirements 2.6**
 */
class LombokMethodNameDeriverDeduplicationPropertiesTest {

    private final LombokMethodNameDeriver deriver = new LombokMethodNameDeriverImpl();

    /**
     * Property 8: Sem duplicatas quando getter explícito existe (negative case)
     *
     * For any field whose derived getter already exists as a MethodDeclaration in the source code,
     * the set of names derived by LombokMethodNameDeriver SHALL NOT contain that getter.
     *
     * Validates: Requirements 2.6
     */
    @Property(tries = 100)
    void getterIsExcludedWhenItExistsInExistingMethods(
            @ForAll("validJavaFieldNames") String fieldName
    ) {
        String expectedGetter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Set<String> existingMethods = Set.of(expectedGetter);
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.GETTER);

        Set<String> derived = deriver.deriveMethodNames(fieldName, "String", annotations, existingMethods);

        assertFalse(derived.contains(expectedGetter),
                "Derived methods should NOT contain getter '" + expectedGetter
                        + "' when it already exists in existingMethods, but got: " + derived);
    }

    /**
     * Property 8: Sem duplicatas quando getter explícito existe (positive case)
     *
     * When the getter is NOT in existingMethods, it IS in the derived set.
     *
     * Validates: Requirements 2.6
     */
    @Property(tries = 100)
    void getterIsIncludedWhenNotInExistingMethods(
            @ForAll("validJavaFieldNames") String fieldName
    ) {
        String expectedGetter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Set<String> existingMethods = Collections.emptySet();
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.GETTER);

        Set<String> derived = deriver.deriveMethodNames(fieldName, "String", annotations, existingMethods);

        assertTrue(derived.contains(expectedGetter),
                "Derived methods should contain getter '" + expectedGetter
                        + "' when it is NOT in existingMethods, but got: " + derived);
    }

    /**
     * Property 8: Sem duplicatas para setter (negative case)
     *
     * For any field whose derived setter already exists in existingMethods,
     * the derived set SHALL NOT contain that setter.
     *
     * Validates: Requirements 2.6
     */
    @Property(tries = 100)
    void setterIsExcludedWhenItExistsInExistingMethods(
            @ForAll("validJavaFieldNames") String fieldName
    ) {
        String expectedSetter = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Set<String> existingMethods = Set.of(expectedSetter);
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.SETTER);

        Set<String> derived = deriver.deriveMethodNames(fieldName, "String", annotations, existingMethods);

        assertFalse(derived.contains(expectedSetter),
                "Derived methods should NOT contain setter '" + expectedSetter
                        + "' when it already exists in existingMethods, but got: " + derived);
    }

    /**
     * Property 8: Sem duplicatas para setter (positive case)
     *
     * When the setter is NOT in existingMethods, it IS in the derived set.
     *
     * Validates: Requirements 2.6
     */
    @Property(tries = 100)
    void setterIsIncludedWhenNotInExistingMethods(
            @ForAll("validJavaFieldNames") String fieldName
    ) {
        String expectedSetter = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Set<String> existingMethods = Collections.emptySet();
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.SETTER);

        Set<String> derived = deriver.deriveMethodNames(fieldName, "String", annotations, existingMethods);

        assertTrue(derived.contains(expectedSetter),
                "Derived methods should contain setter '" + expectedSetter
                        + "' when it is NOT in existingMethods, but got: " + derived);
    }

    /**
     * Property 8: Sem duplicatas para método builder (negative case)
     *
     * For any field whose derived builder method (fieldName itself) already exists in existingMethods,
     * the derived set SHALL NOT contain that builder method.
     *
     * Validates: Requirements 2.6
     */
    @Property(tries = 100)
    void builderMethodIsExcludedWhenItExistsInExistingMethods(
            @ForAll("validJavaFieldNames") String fieldName
    ) {
        // Builder method name equals fieldName (no prefix)
        Set<String> existingMethods = Set.of(fieldName);
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.BUILDER);

        Set<String> derived = deriver.deriveMethodNames(fieldName, "String", annotations, existingMethods);

        assertFalse(derived.contains(fieldName),
                "Derived methods should NOT contain builder method '" + fieldName
                        + "' when it already exists in existingMethods, but got: " + derived);
    }

    /**
     * Property 8: Sem duplicatas para método builder (positive case)
     *
     * When the builder method is NOT in existingMethods, it IS in the derived set.
     *
     * Validates: Requirements 2.6
     */
    @Property(tries = 100)
    void builderMethodIsIncludedWhenNotInExistingMethods(
            @ForAll("validJavaFieldNames") String fieldName
    ) {
        // Builder method name equals fieldName (no prefix)
        Set<String> existingMethods = Collections.emptySet();
        Set<LombokAnnotation> annotations = Set.of(LombokAnnotation.BUILDER);

        Set<String> derived = deriver.deriveMethodNames(fieldName, "String", annotations, existingMethods);

        assertTrue(derived.contains(fieldName),
                "Derived methods should contain builder method '" + fieldName
                        + "' when it is NOT in existingMethods, but got: " + derived);
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
