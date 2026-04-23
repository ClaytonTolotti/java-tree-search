package com.javatreesearch.model;

// Feature: ast-code-search, Property 8: Entradas de Configuração Contêm Todos os Campos

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotBlank;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for the ConfigEntry record.
 * Validates: Requirement 2a.3
 */
class ConfigEntryPropertiesTest {

    /**
     * Property 8: Entradas de Configuração Contêm Todos os Campos
     *
     * For any configuration file containing the search term, each extracted
     * ConfigEntry must contain: non-null key, non-null value, file path,
     * and line number > 0.
     *
     * Validates: Requirement 2a.3
     */
    @Property(tries = 100)
    void configEntryContainsAllMandatoryFields(
            @ForAll @NotBlank String key,
            @ForAll @NotBlank String value,
            @ForAll("validPaths") Path file,
            @ForAll @IntRange(min = 1, max = 100_000) int line
    ) {
        ConfigEntry entry = new ConfigEntry(key, value, file, line);

        assertNotNull(entry.key(), "key must not be null");
        assertNotNull(entry.value(), "value must not be null");
        assertNotNull(entry.file(), "file must not be null");
        assertTrue(entry.line() > 0, "line must be > 0");
    }

    /** Constructing a ConfigEntry with null key must throw NullPointerException. */
    @Example
    void nullKeyThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
            new ConfigEntry(null, "someValue", Path.of("app.properties"), 1)
        );
    }

    /** Constructing a ConfigEntry with null value must throw NullPointerException. */
    @Example
    void nullValueThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
            new ConfigEntry("some.key", null, Path.of("app.properties"), 1)
        );
    }

    /** Constructing a ConfigEntry with line <= 0 must throw IllegalArgumentException. */
    @Example
    void nonPositiveLineThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            new ConfigEntry("some.key", "someValue", Path.of("app.properties"), 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new ConfigEntry("some.key", "someValue", Path.of("app.properties"), -1)
        );
    }

    @Provide
    Arbitrary<Path> validPaths() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(1)
            .ofMaxLength(20)
            .map(s -> Path.of(s + ".properties"));
    }
}
