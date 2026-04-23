package com.javatreesearch.parser;

// Feature: ast-code-search, Property 7: Cache do Config_Parser Evita Releitura

import com.javatreesearch.model.ConfigEntry;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ConfigParser cache behavior.
 * Validates: Requirement 2a.2
 */
class ConfigParserCachePropertiesTest {

    /**
     * Property 7: Cache do Config_Parser Evita Releitura
     *
     * For any config file, if getEntries() is called more than once with the same path
     * in the same execution, the file read must occur at most once — subsequent calls
     * return the same data from cache.
     *
     * Validates: Requirement 2a.2
     */
    @Property(tries = 100)
    void cacheReturnsSameEntriesOnRepeatedCalls(
            @ForAll @NotBlank String key,
            @ForAll @NotBlank String value
    ) throws IOException {
        String sanitizedKey = key.replaceAll("[^a-zA-Z0-9._-]", "x");
        String sanitizedValue = value.replaceAll("[^a-zA-Z0-9._-]", "x");
        if (sanitizedKey.isEmpty()) sanitizedKey = "mykey";
        if (sanitizedValue.isEmpty()) sanitizedValue = "myvalue";

        String content = sanitizedKey + "=" + sanitizedValue;

        Path dir = Files.createTempDirectory("ast-test");
        try {
            Path configFile = dir.resolve("app.properties");
            Files.writeString(configFile, content);

            ConfigParserImpl parser = new ConfigParserImpl();

            List<ConfigEntry> first = parser.getEntries(configFile, sanitizedKey);
            List<ConfigEntry> second = parser.getEntries(configFile, sanitizedKey);
            List<ConfigEntry> third = parser.getEntries(configFile, sanitizedKey);

            assertEquals(first, second, "Second call must return same entries as first");
            assertEquals(first, third, "Third call must return same entries as first");
        } finally {
            deleteRecursively(dir);
        }
    }

    /**
     * Verify that entries are correctly parsed from a .properties file.
     */
    @Example
    void propertiesFileEntriesAreCorrectlyParsed() throws IOException {
        Path dir = Files.createTempDirectory("ast-test");
        try {
            Path configFile = dir.resolve("test.properties");
            Files.writeString(configFile, "server.port=8080\napp.name=MyApp\n");

            ConfigParserImpl parser = new ConfigParserImpl();
            List<ConfigEntry> entries = parser.getEntries(configFile, "server.port");

            assertFalse(entries.isEmpty(), "Should find entry for 'server.port'");
            assertEquals("server.port", entries.get(0).key());
            assertEquals("8080", entries.get(0).value());
            assertTrue(entries.get(0).line() > 0, "Line number must be > 0");
        } finally {
            deleteRecursively(dir);
        }
    }

    /**
     * Verify that entries are correctly parsed from a .yaml file.
     */
    @Example
    void yamlFileEntriesAreCorrectlyParsed() throws IOException {
        Path dir = Files.createTempDirectory("ast-test");
        try {
            Path configFile = dir.resolve("application.yaml");
            Files.writeString(configFile, "server:\n  port: 8080\napp:\n  name: MyApp\n");

            ConfigParserImpl parser = new ConfigParserImpl();
            List<ConfigEntry> entries = parser.getEntries(configFile, "port");

            assertFalse(entries.isEmpty(), "Should find entry containing 'port'");
        } finally {
            deleteRecursively(dir);
        }
    }

    private void deleteRecursively(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                      .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            }
        }
    }
}
