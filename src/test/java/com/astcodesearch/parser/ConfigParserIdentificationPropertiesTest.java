package com.javatreesearch.parser;

// Feature: ast-code-search, Property 6: Config_Parser Identifica Todos os Arquivos de Configuração

import net.jqwik.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ConfigParser file identification.
 * Validates: Requirement 2a.1
 */
class ConfigParserIdentificationPropertiesTest {

    /**
     * Property 6: Config_Parser Identifica Todos os Arquivos de Configuração
     *
     * For any directory containing files with .properties, .yaml, or .yml extensions,
     * the Config_Parser must include all those files in the list of config files to process.
     *
     * Validates: Requirement 2a.1
     */
    @Property(tries = 100)
    void allConfigFilesAreIdentified(
            @ForAll("configExtensions") String extension
    ) throws IOException {
        Path dir = Files.createTempDirectory("ast-test");
        try {
            Path configFile = dir.resolve("config" + extension);
            Files.writeString(configFile, "key=value");

            ConfigParserImpl parser = new ConfigParserImpl();
            List<Path> configFiles = parser.listConfigFiles(dir);

            assertTrue(configFiles.contains(configFile),
                "Config file with extension '" + extension + "' must be identified");
        } finally {
            deleteRecursively(dir);
        }
    }

    @Property(tries = 100)
    void nonConfigFilesAreNotIdentified(
            @ForAll("nonConfigExtensions") String extension
    ) throws IOException {
        Path dir = Files.createTempDirectory("ast-test");
        try {
            Path nonConfigFile = dir.resolve("file" + extension);
            Files.writeString(nonConfigFile, "some content");

            ConfigParserImpl parser = new ConfigParserImpl();
            List<Path> configFiles = parser.listConfigFiles(dir);

            assertFalse(configFiles.contains(nonConfigFile),
                "File with extension '" + extension + "' must NOT be identified as config file");
        } finally {
            deleteRecursively(dir);
        }
    }

    @Provide
    Arbitrary<String> configExtensions() {
        return Arbitraries.of(".properties", ".yaml", ".yml");
    }

    @Provide
    Arbitrary<String> nonConfigExtensions() {
        return Arbitraries.of(".java", ".txt", ".xml", ".json", ".md", ".py");
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
