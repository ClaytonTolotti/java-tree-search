package com.javatreesearch.resolver;

// Feature: ast-code-search, Property 9: Chaves de Configuração São Resolvidas como CONFIG_KEY

import com.javatreesearch.model.ConfigEntry;
import com.javatreesearch.model.Reference;
import com.javatreesearch.model.ReferenceType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotBlank;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ReferenceResolver CONFIG_KEY resolution.
 * Validates: Requirement 2a.4
 */
class ReferenceResolverConfigKeyPropertiesTest {

    /**
     * Property 9: Chaves de Configuração São Resolvidas como CONFIG_KEY
     *
     * For any config entry where the search term matches the key,
     * the Reference extracted by ReferenceResolver must have type == CONFIG_KEY.
     *
     * Validates: Requirement 2a.4
     */
    @Property(tries = 100)
    void configEntryKeyIsResolvedAsConfigKey(
            @ForAll @NotBlank String key,
            @ForAll @NotBlank String value,
            @ForAll("validPaths") Path file,
            @ForAll @IntRange(min = 1, max = 10000) int line
    ) {
        ConfigEntry entry = new ConfigEntry(key, value, file, line);
        ReferenceResolverImpl resolver = new ReferenceResolverImpl();

        Reference ref = resolver.resolveFromConfigEntry(entry);

        assertNotNull(ref, "Resolved reference must not be null");
        assertEquals(ReferenceType.CONFIG_KEY, ref.type(),
            "Config entry key must be resolved as CONFIG_KEY");
        assertEquals(key, ref.name(), "Reference name must match config entry key");
        assertEquals(file, ref.file(), "Reference file must match config entry file");
        assertEquals(line, ref.line(), "Reference line must match config entry line");
        assertEquals(0, ref.column(), "Config key reference column must be 0");
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
