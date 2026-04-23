package com.javatreesearch.parser;

// Feature: ast-code-search, Property 5: Extensões Não Reconhecidas São Ignoradas

import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for AstParser unrecognized extension handling.
 * Validates: Requirement 2.11
 */
class AstParserExtensionPropertiesTest {

    /**
     * Property 5: Extensões Não Reconhecidas São Ignoradas
     *
     * For any file with an extension other than .java, .properties, .yaml, .yml,
     * the system must not attempt to parse or process the file, and no error must be logged.
     *
     * Validates: Requirement 2.11
     */
    @Property(tries = 100)
    void unrecognizedExtensionsAreNotListedByAstParser(
            @ForAll("unrecognizedExtensions") String extension
    ) throws IOException {
        Path dir = Files.createTempDirectory("ast-test");
        try {
            Path file = dir.resolve("testfile" + extension);
            Files.writeString(file, "some content");

            AstParserImpl parser = new AstParserImpl();
            List<Path> javaFiles = parser.listJavaFiles(dir);

            assertFalse(javaFiles.contains(file),
                "File with extension '" + extension + "' must not be listed by listJavaFiles");
        } finally {
            deleteRecursively(dir);
        }
    }

    @Property(tries = 100)
    void onlyJavaFilesAreListedByAstParser(
            @ForAll @NotBlank String baseName
    ) throws IOException {
        String sanitized = baseName.replaceAll("[^a-zA-Z0-9]", "X");
        if (sanitized.isEmpty()) sanitized = "File";
        if (sanitized.length() > 40) sanitized = sanitized.substring(0, 40);

        Path dir = Files.createTempDirectory("ast-test");
        try {
            Path javaFile = dir.resolve(sanitized + ".java");
            Path txtFile = dir.resolve(sanitized + ".txt");
            Files.writeString(javaFile, "public class " + sanitized + " {}");
            Files.writeString(txtFile, "some text");

            AstParserImpl parser = new AstParserImpl();
            List<Path> javaFiles = parser.listJavaFiles(dir);

            assertTrue(javaFiles.contains(javaFile), ".java file must be listed");
            assertFalse(javaFiles.contains(txtFile), ".txt file must NOT be listed");
        } finally {
            deleteRecursively(dir);
        }
    }

    @Provide
    Arbitrary<String> unrecognizedExtensions() {
        return Arbitraries.of(".txt", ".xml", ".json", ".md", ".py", ".rb", ".go", ".ts", ".js", ".html", ".css", ".sh");
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
