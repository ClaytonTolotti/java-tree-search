package com.javatreesearch.parser;

// Feature: cloudformation-yaml-support, Property 4: Pesquisabilidade de referências

import com.javatreesearch.model.ConfigEntry;
import net.jqwik.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ConfigParser reference searchability.
 *
 * Property 4: Pesquisabilidade de referências
 * For any YAML with `!Ref X` (where X is a non-empty string), calling
 * configParser.getEntries(file, "X") must return at least one ConfigEntry
 * whose key or value contains "X".
 *
 * Validates: Requirements 12.1
 */
class ConfigParserRefSearchabilityPropertiesTest {

    /**
     * Property 4: For any YAML containing `!Ref <resourceName>`, getEntries(file, resourceName)
     * returns at least one ConfigEntry whose key or value contains the resource name.
     *
     * Validates: Requirements 12.1
     */
    @Property(tries = 100)
    void refResourceNameIsSearchable(
            @ForAll("resourceNames") String resourceName
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("cfn-ref-search-test");
        try {
            String yaml = buildYamlWithRef(resourceName);
            Path yamlFile = tempDir.resolve("template.yaml");
            Files.writeString(yamlFile, yaml);

            ConfigParserImpl parser = new ConfigParserImpl();
            List<ConfigEntry> entries = parser.getEntries(yamlFile, resourceName);

            assertFalse(entries.isEmpty(),
                    "getEntries must return at least one ConfigEntry for resource name '" + resourceName +
                    "'. YAML:\n" + yaml);

            boolean found = entries.stream().anyMatch(e ->
                    e.key().contains(resourceName) || e.value().contains(resourceName));

            assertTrue(found,
                    "At least one ConfigEntry must have key or value containing '" + resourceName +
                    "'. Entries: " + entries + ". YAML:\n" + yaml);
        } finally {
            deleteRecursively(tempDir);
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
     * Builds a minimal CloudFormation-style YAML with a !Ref tag for the given resource name.
     */
    private String buildYamlWithRef(String resourceName) {
        return "Resources:\n" +
               "  MyResource:\n" +
               "    Properties:\n" +
               "      RefValue: !Ref " + resourceName + "\n";
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
