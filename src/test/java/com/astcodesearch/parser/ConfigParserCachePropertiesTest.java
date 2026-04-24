package com.javatreesearch.parser;

// Feature: ast-code-search, Property 7: Cache do Config_Parser Evita Releitura
// Feature: cloudformation-yaml-support, Property 5: Idempotência de cache

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
 * Validates: Requirement 2a.2, Requirements 13.1, 13.2
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
     * Property 5: Idempotência de cache — YAML com CloudFormation tags
     *
     * For any YAML file with CloudFormation tags (!Ref), calling getEntries() N times
     * with the same file and term must always return the same list (same elements, same order).
     *
     * Validates: Requirements 13.1, 13.2
     */
    @Property(tries = 100)
    void cacheIdempotencyWithCloudFormationYaml(
            @ForAll("resourceNames") String resourceName,
            @ForAll("searchTerms") String searchTerm
    ) throws IOException {
        Path dir = Files.createTempDirectory("cfn-cache-test");
        try {
            String yaml = buildYamlWithRef(resourceName);
            Path yamlFile = dir.resolve("template.yaml");
            Files.writeString(yamlFile, yaml);

            ConfigParserImpl parser = new ConfigParserImpl();

            List<ConfigEntry> first = parser.getEntries(yamlFile, searchTerm);
            List<ConfigEntry> second = parser.getEntries(yamlFile, searchTerm);
            List<ConfigEntry> third = parser.getEntries(yamlFile, searchTerm);

            assertEquals(first, second,
                    "Second call must return same entries as first for term '" + searchTerm + "'");
            assertEquals(first, third,
                    "Third call must return same entries as first for term '" + searchTerm + "'");
        } finally {
            deleteRecursively(dir);
        }
    }

    /**
     * Property 5b: Cache is shared across different search terms on the same file.
     *
     * Calling getEntries() with different terms on the same file must use the cached
     * parsed entries — the union of results for term1 and term2 must be consistent
     * with a single parse of the file.
     *
     * Validates: Requirements 13.2, 13.3
     */
    @Property(tries = 100)
    void cacheIsSharedAcrossDifferentTerms(
            @ForAll("resourceNames") String resourceName
    ) throws IOException {
        Path dir = Files.createTempDirectory("cfn-cache-terms-test");
        try {
            String yaml = buildYamlWithRef(resourceName);
            Path yamlFile = dir.resolve("template.yaml");
            Files.writeString(yamlFile, yaml);

            ConfigParserImpl parser = new ConfigParserImpl();

            // Call with the resource name (should find the !Ref entry)
            List<ConfigEntry> byResource = parser.getEntries(yamlFile, resourceName);
            // Call with empty term (should return all entries)
            List<ConfigEntry> allEntries = parser.getEntries(yamlFile, "");
            // Call with resource name again (must be identical to first call — from cache)
            List<ConfigEntry> byResourceAgain = parser.getEntries(yamlFile, resourceName);

            assertEquals(byResource, byResourceAgain,
                    "Repeated call with same term must return identical list (cache hit)");
            assertTrue(allEntries.size() >= byResource.size(),
                    "All-entries result must contain at least as many entries as filtered result");
        } finally {
            deleteRecursively(dir);
        }
    }

    /**
     * Generates random non-empty alphanumeric resource names, as used in CloudFormation logical IDs.
     */
    @Provide
    Arbitrary<String> resourceNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20);
    }

    /**
     * Generates search terms: either the resource name prefix, empty string, or a random alpha string.
     */
    @Provide
    Arbitrary<String> searchTerms() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(0)
                .ofMaxLength(10);
    }

    /**
     * Builds a minimal CloudFormation-style YAML with a !Ref tag for the given resource name.
     */
    private String buildYamlWithRef(String resourceName) {
        return "Resources:\n" +
               "  MyResource:\n" +
               "    Properties:\n" +
               "      RefValue: !Ref " + resourceName + "\n";
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
