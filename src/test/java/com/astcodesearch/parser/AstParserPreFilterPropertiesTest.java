package com.javatreesearch.parser;

// Feature: ast-code-search, Property 4: Pré-filtro Controla o Parse Completo

import com.github.javaparser.ast.CompilationUnit;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for AstParser pre-filter behavior.
 * Validates: Requirements 2.3, 2.4, 2.5, 6.3
 */
class AstParserPreFilterPropertiesTest {

    /**
     * Property 4: Pré-filtro Controla o Parse Completo
     *
     * For any Java file and any search term:
     * - If the file CONTAINS the term textually, getAst() must return a present AST and cache it.
     * - If the file does NOT contain the term textually, getAst() must return Optional.empty()
     *   without invoking the full parser.
     *
     * Validates: Requirements 2.3, 2.4, 2.5, 6.3
     */
    @Property(tries = 100)
    void preFilterPositiveReturnsPresentAst(
            @ForAll @NotBlank String identifier
    ) throws IOException {
        String sanitized = identifier.replaceAll("[^a-zA-Z0-9]", "X");
        if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
            sanitized = "C" + sanitized;
        }
        if (sanitized.length() > 40) sanitized = sanitized.substring(0, 40);
        String term = sanitized;
        String javaContent = "public class " + term + " { }";

        Path dir = Files.createTempDirectory("ast-test");
        try {
            Path javaFile = dir.resolve(term + ".java");
            Files.writeString(javaFile, javaContent);

            AstParserImpl parser = new AstParserImpl();
            Optional<CompilationUnit> result = parser.getAst(javaFile, term);

            assertTrue(result.isPresent(), "File containing the term must return a present AST");
        } finally {
            deleteRecursively(dir);
        }
    }

    @Property(tries = 100)
    void preFilterNegativeReturnsEmpty(
            @ForAll @NotBlank String identifier
    ) throws IOException {
        String sanitized = identifier.replaceAll("[^a-zA-Z0-9]", "X");
        if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
            sanitized = "C" + sanitized;
        }
        if (sanitized.length() > 40) sanitized = sanitized.substring(0, 40);
        String term = sanitized;
        String absentTerm = "ABSENT_TERM_XYZ_99999";
        String javaContent = "public class " + term + " { }";

        Path dir = Files.createTempDirectory("ast-test");
        try {
            Path javaFile = dir.resolve(term + ".java");
            Files.writeString(javaFile, javaContent);

            AstParserImpl parser = new AstParserImpl();
            Optional<CompilationUnit> result = parser.getAst(javaFile, absentTerm);

            assertTrue(result.isEmpty(), "File NOT containing the term must return Optional.empty()");
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
