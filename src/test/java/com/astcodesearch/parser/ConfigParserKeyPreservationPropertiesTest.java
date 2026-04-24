package com.javatreesearch.parser;

// Feature: cloudformation-yaml-support, Property 2: Preservação de chaves (equivalência com parser anterior)

import com.javatreesearch.model.ConfigEntry;
import net.jqwik.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ConfigParser key preservation.
 *
 * Property 2: Preservação de chaves (equivalência com parser anterior)
 * For any simple key-value YAML map (no CFN tags), the new ConfigParserImpl must:
 * 1. Return a non-null ConfigEntry list with the same number of entries as the input map
 * 2. Preserve all keys in the ConfigEntry list
 * 3. Preserve all values as-is in ConfigEntry.value()
 *
 * Validates: Requirements 11.5
 */
class ConfigParserKeyPreservationPropertiesTest {

    /**
     * Property 2a: All keys from a simple YAML map appear in the ConfigEntry list.
     *
     * Validates: Requirements 11.5
     */
    @Property(tries = 100)
    void allKeysArePreservedInConfigEntries(
            @ForAll("simpleYamlMaps") Map<String, String> inputMap
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("cfn-key-test");
        try {
            String yaml = serializeToYaml(inputMap);
            Path yamlFile = tempDir.resolve("config.yaml");
            Files.writeString(yamlFile, yaml);

            ConfigParserImpl parser = new ConfigParserImpl();
            List<ConfigEntry> entries = parser.getEntries(yamlFile, "");

            assertNotNull(entries, "ConfigEntry list must not be null");

            Set<String> entryKeys = entries.stream()
                    .map(ConfigEntry::key)
                    .collect(Collectors.toSet());

            for (String key : inputMap.keySet()) {
                assertTrue(entryKeys.contains(key),
                        "Key '" + key + "' must appear in ConfigEntry list. Found keys: " + entryKeys);
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * Property 2b: All values from a simple YAML map are preserved in ConfigEntry.value().
     *
     * Validates: Requirements 11.5
     */
    @Property(tries = 100)
    void allValuesArePreservedInConfigEntries(
            @ForAll("simpleYamlMaps") Map<String, String> inputMap
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("cfn-key-test");
        try {
            String yaml = serializeToYaml(inputMap);
            Path yamlFile = tempDir.resolve("config.yaml");
            Files.writeString(yamlFile, yaml);

            ConfigParserImpl parser = new ConfigParserImpl();
            List<ConfigEntry> entries = parser.getEntries(yamlFile, "");

            assertNotNull(entries, "ConfigEntry list must not be null");

            Map<String, String> entryMap = entries.stream()
                    .collect(Collectors.toMap(ConfigEntry::key, ConfigEntry::value, (a, b) -> a));

            for (Map.Entry<String, String> input : inputMap.entrySet()) {
                String key = input.getKey();
                String expectedValue = input.getValue();
                assertTrue(entryMap.containsKey(key),
                        "Key '" + key + "' must be present in entries");
                assertEquals(expectedValue, entryMap.get(key),
                        "Value for key '" + key + "' must be preserved as-is");
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * Property 2c: The ConfigEntry list is non-null and has the same number of entries as the input map.
     *
     * Validates: Requirements 11.5
     */
    @Property(tries = 100)
    void entryCountMatchesInputMapSize(
            @ForAll("simpleYamlMaps") Map<String, String> inputMap
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("cfn-key-test");
        try {
            String yaml = serializeToYaml(inputMap);
            Path yamlFile = tempDir.resolve("config.yaml");
            Files.writeString(yamlFile, yaml);

            ConfigParserImpl parser = new ConfigParserImpl();
            List<ConfigEntry> entries = parser.getEntries(yamlFile, "");

            assertNotNull(entries, "ConfigEntry list must not be null");
            assertEquals(inputMap.size(), entries.size(),
                    "Number of ConfigEntries must match number of keys in input map. " +
                    "Expected: " + inputMap.size() + ", got: " + entries.size() +
                    ". YAML:\n" + yaml);
        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * Generates simple key-value maps with alphanumeric keys and values (no CFN tags).
     * Keys are constrained to valid YAML identifiers (alphabetic only, to avoid YAML parsing ambiguity).
     */
    @Provide
    Arbitrary<Map<String, String>> simpleYamlMaps() {
        Arbitrary<String> keys = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10);

        Arbitrary<String> values = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20);

        return Arbitraries.maps(keys, values)
                .ofMinSize(1)
                .ofMaxSize(5);
    }

    /**
     * Serializes a map to a flat YAML string (no nesting, no CFN tags).
     */
    private String serializeToYaml(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    private void deleteRecursively(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder())
                      .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            }
        }
    }
}
