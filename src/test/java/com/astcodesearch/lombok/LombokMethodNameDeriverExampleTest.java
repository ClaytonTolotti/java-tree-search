package com.javatreesearch.lombok;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example-based tests for LombokMethodNameDeriver with concrete field and class cases.
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
class LombokMethodNameDeriverExampleTest {

    private final LombokMethodNameDeriver deriver = new LombokMethodNameDeriverImpl();

    // -------------------------------------------------------------------------
    // Req 2.1 + 2.3: @Data on non-boolean field → getter and setter derived
    // -------------------------------------------------------------------------

    /**
     * Req 2.1, 2.3: Field `pathDownload` with @Data → derived names include
     * `getPathDownload` and `setPathDownload`.
     */
    @Test
    void fieldPathDownloadWithDataDerivesGetterAndSetter() {
        Set<String> derived = deriver.deriveMethodNames(
                "pathDownload",
                "String",
                Set.of(LombokAnnotation.DATA),
                Set.of()
        );

        assertTrue(derived.contains("getPathDownload"),
                "Expected 'getPathDownload' in derived names, but got: " + derived);
        assertTrue(derived.contains("setPathDownload"),
                "Expected 'setPathDownload' in derived names, but got: " + derived);
    }

    // -------------------------------------------------------------------------
    // Req 2.2: Boolean field with @Getter → "is" prefix
    // -------------------------------------------------------------------------

    /**
     * Req 2.2: Boolean field `active` with @Getter → derived name is `isActive`.
     */
    @Test
    void booleanFieldActiveWithGetterDerivesIsActive() {
        Set<String> derived = deriver.deriveMethodNames(
                "active",
                "boolean",
                Set.of(LombokAnnotation.GETTER),
                Set.of()
        );

        assertTrue(derived.contains("isActive"),
                "Expected 'isActive' in derived names for boolean field, but got: " + derived);
        assertFalse(derived.contains("getActive"),
                "Should not derive 'getActive' for boolean field, but got: " + derived);
    }

    // -------------------------------------------------------------------------
    // Req 2.6: Existing getter in existingMethods → getter NOT duplicated
    // -------------------------------------------------------------------------

    /**
     * Req 2.6: Field `name` with @Getter and explicit getter `getName` in
     * existingMethods → getter NOT included in derived set.
     */
    @Test
    void fieldNameWithExplicitGetterNotDuplicated() {
        Set<String> derived = deriver.deriveMethodNames(
                "name",
                "String",
                Set.of(LombokAnnotation.GETTER),
                Set.of("getName")
        );

        assertFalse(derived.contains("getName"),
                "Getter 'getName' already exists in source; should not be duplicated, but got: " + derived);
    }

    // -------------------------------------------------------------------------
    // Req 2.4: @Builder → field name without prefix
    // -------------------------------------------------------------------------

    /**
     * Req 2.4: Field `id` with @Builder → derived name is `id` (no prefix).
     */
    @Test
    void fieldIdWithBuilderDerivesFieldNameAsMethod() {
        Set<String> derived = deriver.deriveMethodNames(
                "id",
                "Long",
                Set.of(LombokAnnotation.BUILDER),
                Set.of()
        );

        assertTrue(derived.contains("id"),
                "Expected 'id' (no prefix) in derived names for @Builder field, but got: " + derived);
        assertFalse(derived.contains("getId"),
                "Should not derive 'getId' for @Builder-only field, but got: " + derived);
    }

    // -------------------------------------------------------------------------
    // Req 2.5: @AllArgsConstructor → deriveConstructorNames returns class name
    // -------------------------------------------------------------------------

    /**
     * Req 2.5: Class `UserService` with @AllArgsConstructor →
     * deriveConstructorNames returns {"UserService"}.
     */
    @Test
    void classUserServiceWithAllArgsConstructorDerivesClassName() {
        Set<String> derived = deriver.deriveConstructorNames(
                "UserService",
                Set.of(LombokAnnotation.ALL_ARGS_CONSTRUCTOR)
        );

        assertEquals(Set.of("UserService"), derived,
                "Expected {\"UserService\"} for @AllArgsConstructor, but got: " + derived);
    }

    // -------------------------------------------------------------------------
    // Req 2.5: @RequiredArgsConstructor → deriveConstructorNames returns class name
    // -------------------------------------------------------------------------

    /**
     * Req 2.5: Class `Config` with @RequiredArgsConstructor →
     * deriveConstructorNames returns {"Config"}.
     */
    @Test
    void classConfigWithRequiredArgsConstructorDerivesClassName() {
        Set<String> derived = deriver.deriveConstructorNames(
                "Config",
                Set.of(LombokAnnotation.REQUIRED_ARGS_CONSTRUCTOR)
        );

        assertEquals(Set.of("Config"), derived,
                "Expected {\"Config\"} for @RequiredArgsConstructor, but got: " + derived);
    }

    // -------------------------------------------------------------------------
    // No Lombok annotations → empty set
    // -------------------------------------------------------------------------

    /**
     * Field `count` with no Lombok annotations → derived set is empty.
     */
    @Test
    void fieldWithNoLombokAnnotationsDerivesEmptySet() {
        Set<String> derived = deriver.deriveMethodNames(
                "count",
                "int",
                Set.of(),
                Set.of()
        );

        assertTrue(derived.isEmpty(),
                "Expected empty derived set for field with no Lombok annotations, but got: " + derived);
    }

    // -------------------------------------------------------------------------
    // Req 2.1: capitalize handles uppercase first letter correctly
    // -------------------------------------------------------------------------

    /**
     * Req 2.1: Field `url` with @Data → derived names include `getUrl` and `setUrl`
     * (capitalize: 'u' → 'U', rest unchanged).
     */
    @Test
    void fieldUrlWithDataDerivesGetUrlAndSetUrl() {
        Set<String> derived = deriver.deriveMethodNames(
                "url",
                "String",
                Set.of(LombokAnnotation.DATA),
                Set.of()
        );

        assertTrue(derived.contains("getUrl"),
                "Expected 'getUrl' in derived names, but got: " + derived);
        assertTrue(derived.contains("setUrl"),
                "Expected 'setUrl' in derived names, but got: " + derived);
    }
}
