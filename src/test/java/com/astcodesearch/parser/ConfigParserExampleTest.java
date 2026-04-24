package com.javatreesearch.parser;

import com.javatreesearch.model.ConfigEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example-based tests for ConfigParserImpl with CloudFormation YAML support.
 * Validates: Requirements 11.1, 11.4, 11.5, 14.1, 14.3, 14.4
 */
class ConfigParserExampleTest {

    @TempDir
    Path tempDir;

    /**
     * Req 11.1, 11.4: YAML with !Ref tag → ConfigEntry value = "Ref:MyBucket"
     */
    @Test
    void yamlWithRefTagProducesCorrectConfigEntry() throws IOException {
        Path yamlFile = tempDir.resolve("template.yaml");
        Files.writeString(yamlFile,
            "BucketRef: !Ref MyBucket\n"
        );

        ConfigParserImpl parser = new ConfigParserImpl();
        List<ConfigEntry> entries = parser.getEntries(yamlFile, "BucketRef");

        assertFalse(entries.isEmpty(), "Should find entry for 'BucketRef'");
        ConfigEntry entry = entries.get(0);
        assertEquals("BucketRef", entry.key());
        assertEquals("Ref:MyBucket", entry.value());
    }

    /**
     * Req 11.1, 11.4: YAML with !Sub tag → ConfigEntry value = "Sub:arn:aws:s3:::${BucketName}"
     */
    @Test
    void yamlWithSubTagProducesCorrectConfigEntry() throws IOException {
        Path yamlFile = tempDir.resolve("template.yaml");
        Files.writeString(yamlFile,
            "BucketArn: !Sub \"arn:aws:s3:::${BucketName}\"\n"
        );

        ConfigParserImpl parser = new ConfigParserImpl();
        List<ConfigEntry> entries = parser.getEntries(yamlFile, "BucketArn");

        assertFalse(entries.isEmpty(), "Should find entry for 'BucketArn'");
        ConfigEntry entry = entries.get(0);
        assertEquals("BucketArn", entry.key());
        assertEquals("Sub:arn:aws:s3:::${BucketName}", entry.value());
    }

    /**
     * Req 11.1, 11.4: YAML with !GetAtt tag (scalar form) → ConfigEntry value = "GetAtt:MyBucket.Arn"
     */
    @Test
    void yamlWithGetAttTagProducesCorrectConfigEntry() throws IOException {
        Path yamlFile = tempDir.resolve("template.yaml");
        Files.writeString(yamlFile,
            "BucketArnAttr: !GetAtt MyBucket.Arn\n"
        );

        ConfigParserImpl parser = new ConfigParserImpl();
        List<ConfigEntry> entries = parser.getEntries(yamlFile, "BucketArnAttr");

        assertFalse(entries.isEmpty(), "Should find entry for 'BucketArnAttr'");
        ConfigEntry entry = entries.get(0);
        assertEquals("BucketArnAttr", entry.key());
        assertEquals("GetAtt:MyBucket.Arn", entry.value());
    }

    /**
     * Req 11.1, 11.4: YAML with !Join tag → ConfigEntry value starts with "Join:"
     */
    @Test
    void yamlWithJoinTagProducesCorrectConfigEntry() throws IOException {
        Path yamlFile = tempDir.resolve("template.yaml");
        Files.writeString(yamlFile,
            "JoinedValue: !Join [\"-\", [\"a\", \"b\"]]\n"
        );

        ConfigParserImpl parser = new ConfigParserImpl();
        List<ConfigEntry> entries = parser.getEntries(yamlFile, "JoinedValue");

        assertFalse(entries.isEmpty(), "Should find entry for 'JoinedValue'");
        ConfigEntry entry = entries.get(0);
        assertEquals("JoinedValue", entry.key());
        assertTrue(entry.value().startsWith("Join:"), "Value should start with 'Join:' but was: " + entry.value());
    }

    /**
     * Req 11.5: YAML without CloudFormation tags → plain string values preserved
     */
    @Test
    void yamlWithoutCfnTagsPreservesPlainValues() throws IOException {
        Path yamlFile = tempDir.resolve("application.yaml");
        Files.writeString(yamlFile,
            "server:\n"
            + "  port: 8080\n"
            + "app:\n"
            + "  name: MyApp\n"
        );

        ConfigParserImpl parser = new ConfigParserImpl();
        List<ConfigEntry> portEntries = parser.getEntries(yamlFile, "port");
        List<ConfigEntry> nameEntries = parser.getEntries(yamlFile, "name");

        assertFalse(portEntries.isEmpty(), "Should find entry containing 'port'");
        assertFalse(nameEntries.isEmpty(), "Should find entry containing 'name'");

        ConfigEntry portEntry = portEntries.stream()
            .filter(e -> e.key().equals("server.port"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected entry with key 'server.port'"));
        assertEquals("8080", portEntry.value());

        ConfigEntry nameEntry = nameEntries.stream()
            .filter(e -> e.key().equals("app.name"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected entry with key 'app.name'"));
        assertEquals("MyApp", nameEntry.value());
    }

    /**
     * Req 14.1: Invalid YAML → throws ConfigIoException
     */
    @Test
    void invalidYamlThrowsConfigIoException() throws IOException {
        Path yamlFile = tempDir.resolve("invalid.yaml");
        Files.writeString(yamlFile, "key: [unclosed bracket\n  bad: indent: here\n");

        ConfigParserImpl parser = new ConfigParserImpl();
        assertThrows(ConfigParserImpl.ConfigIoException.class,
            () -> parser.getEntries(yamlFile, "key"),
            "Invalid YAML should throw ConfigIoException");
    }

    /**
     * Req 14.3: YAML with non-Map root (list) → returns empty list without exception
     */
    @Test
    void yamlWithListRootReturnsEmptyList() throws IOException {
        Path yamlFile = tempDir.resolve("list-root.yaml");
        Files.writeString(yamlFile, "- item1\n- item2\n- item3\n");

        ConfigParserImpl parser = new ConfigParserImpl();
        List<ConfigEntry> entries = parser.getEntries(yamlFile, "item1");

        assertNotNull(entries, "Result should not be null");
        assertTrue(entries.isEmpty(), "YAML with list root should return empty list");
    }

    /**
     * Req 14.4: Non-existent file → throws ConfigIoException with file path in message
     */
    @Test
    void nonExistentFileThrowsConfigIoExceptionWithPath() {
        Path missingFile = tempDir.resolve("does-not-exist.yaml");

        ConfigParserImpl parser = new ConfigParserImpl();
        ConfigParserImpl.ConfigIoException ex = assertThrows(
            ConfigParserImpl.ConfigIoException.class,
            () -> parser.getEntries(missingFile, "anything"),
            "Non-existent file should throw ConfigIoException"
        );

        assertTrue(ex.getMessage().contains(missingFile.toString()),
            "Exception message should contain the file path, but was: " + ex.getMessage());
        assertEquals(missingFile, ex.getFile(),
            "Exception should expose the file path via getFile()");
    }

    /**
     * Req 14.4: Unreadable file (permission denied) → throws ConfigIoException with file path.
     * Skipped on non-POSIX systems (e.g., Windows).
     */
    @Test
    void unreadableFileThrowsConfigIoExceptionWithPath() throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return; // POSIX permissions not supported on Windows
        }

        Path restrictedFile = tempDir.resolve("restricted.yaml");
        Files.writeString(restrictedFile, "key: value\n");
        Files.setPosixFilePermissions(restrictedFile,
            PosixFilePermissions.fromString("---------"));

        try {
            ConfigParserImpl parser = new ConfigParserImpl();
            ConfigParserImpl.ConfigIoException ex = assertThrows(
                ConfigParserImpl.ConfigIoException.class,
                () -> parser.getEntries(restrictedFile, "key"),
                "Unreadable file should throw ConfigIoException"
            );

            assertTrue(ex.getMessage().contains(restrictedFile.toString()),
                "Exception message should contain the file path, but was: " + ex.getMessage());
        } finally {
            Files.setPosixFilePermissions(restrictedFile,
                PosixFilePermissions.fromString("rw-r--r--"));
        }
    }
}
