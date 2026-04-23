package com.javatreesearch.parser;

// Feature: ast-code-search, Property 3: Cache do AST_Parser Evita Reparse

import com.github.javaparser.ast.CompilationUnit;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for AstParser cache behavior.
 * Validates: Requirements 2.2, 6.1
 */
class AstParserCachePropertiesTest {

    /**
     * Property 3: Cache do AST_Parser Evita Reparse
     *
     * For any valid Java file, if getAst() is called more than once with the same
     * path in the same execution, the full parse must occur at most once — subsequent
     * calls must return the same CompilationUnit instance from cache.
     *
     * Validates: Requirements 2.2, 6.1
     */
    @Property(tries = 100)
    void cacheReturnsSameInstanceOnRepeatedCalls(
            @ForAll @NotBlank String className
    ) throws IOException {
        String sanitized = className.replaceAll("[^a-zA-Z0-9]", "X");
        if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
            sanitized = "C" + sanitized;
        }
        // Truncate to avoid "file name too long" OS error
        if (sanitized.length() > 40) sanitized = sanitized.substring(0, 40);
        String term = sanitized;
        String javaContent = "public class " + term + " { }";

        Path dir = Files.createTempDirectory("ast-test");
        try {
            Path javaFile = dir.resolve(term + ".java");
            Files.writeString(javaFile, javaContent);

            AstParserImpl parser = new AstParserImpl();

            Optional<CompilationUnit> first = parser.getAst(javaFile, term);
            Optional<CompilationUnit> second = parser.getAst(javaFile, term);
            Optional<CompilationUnit> third = parser.getAst(javaFile, term);

            assertTrue(first.isPresent(), "First call should return a CompilationUnit");
            assertTrue(second.isPresent(), "Second call should return a CompilationUnit");
            assertTrue(third.isPresent(), "Third call should return a CompilationUnit");

            assertSame(first.get(), second.get(), "Second call must return same cached instance");
            assertSame(first.get(), third.get(), "Third call must return same cached instance");
        } finally {
            deleteRecursively(dir);
        }
    }

    /**
     * Pre-filter negative: file not containing the term returns empty without caching.
     */
    @Example
    void preFilterNegativeReturnsEmpty() throws IOException {
        Path dir = Files.createTempDirectory("ast-test");
        try {
            String javaContent = "public class Foo { }";
            Path javaFile = dir.resolve("Foo.java");
            Files.writeString(javaFile, javaContent);

            AstParserImpl parser = new AstParserImpl();
            Optional<CompilationUnit> result = parser.getAst(javaFile, "NonExistentTerm");

            assertTrue(result.isEmpty(), "Pre-filter negative should return Optional.empty()");
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
