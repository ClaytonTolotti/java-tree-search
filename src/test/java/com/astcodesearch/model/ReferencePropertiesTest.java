package com.javatreesearch.model;

// Feature: ast-code-search, Property 11: Referências de Definição Contêm Todos os Campos Obrigatórios

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotBlank;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for the Reference record.
 * Validates: Requirement 3.2
 */
class ReferencePropertiesTest {

    /**
     * Property 11: Referências de Definição Contêm Todos os Campos Obrigatórios
     *
     * For any valid Reference constructed with non-null name, valid ReferenceType,
     * non-null Path, line > 0, and column >= 0, all mandatory fields must be
     * non-null and within valid ranges.
     *
     * Validates: Requirement 3.2
     */
    @Property(tries = 100)
    void referenceContainsAllMandatoryFields(
            @ForAll @NotBlank String name,
            @ForAll ReferenceType type,
            @ForAll("validPaths") Path file,
            @ForAll @IntRange(min = 1, max = 100_000) int line,
            @ForAll @IntRange(min = 0, max = 10_000) int column
    ) {
        Reference ref = new Reference(name, type, file, line, column);

        assertNotNull(ref.name(), "name must not be null");
        assertNotNull(ref.type(), "type must not be null");
        assertNotNull(ref.file(), "file must not be null");
        assertTrue(ref.line() > 0, "line must be > 0");
        assertTrue(ref.column() >= 0, "column must be >= 0");

        // type must be one of the definition types
        ReferenceType t = ref.type();
        assertTrue(
            t == ReferenceType.VARIABLE ||
            t == ReferenceType.CONSTANT ||
            t == ReferenceType.METHOD   ||
            t == ReferenceType.CLASS    ||
            t == ReferenceType.CONFIG_KEY,
            "type must be a valid ReferenceType"
        );
    }

    /** Constructing a Reference with null name must throw NullPointerException. */
    @Example
    void nullNameThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
            new Reference(null, ReferenceType.VARIABLE, Path.of("Foo.java"), 1, 0)
        );
    }

    /** Constructing a Reference with line <= 0 must throw IllegalArgumentException. */
    @Example
    void nonPositiveLineThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            new Reference("foo", ReferenceType.METHOD, Path.of("Foo.java"), 0, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new Reference("foo", ReferenceType.METHOD, Path.of("Foo.java"), -1, 0)
        );
    }

    /** Constructing a Reference with column < 0 must throw IllegalArgumentException. */
    @Example
    void negativeColumnThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            new Reference("foo", ReferenceType.CLASS, Path.of("Foo.java"), 1, -1)
        );
    }

    @Provide
    Arbitrary<Path> validPaths() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(1)
            .ofMaxLength(20)
            .map(s -> Path.of(s + ".java"));
    }
}
