package com.javatreesearch.cli;

// Feature: ast-code-search, Property 2: Rejeição de Profundidade Inválida

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for CLI depth parameter validation.
 * Validates: Requirement 1.7
 */
class AstSearchCommandDepthPropertiesTest {

    /**
     * Property 2: Rejeição de Profundidade Inválida
     *
     * For any string that does not represent a positive integer provided as the third
     * parameter, the SearchEngine must exit with a non-zero exit code and display an error message.
     *
     * Validates: Requirement 1.7
     */
    @Property(tries = 100)
    void nonPositiveIntegerDepthIsRejected(
            @ForAll("invalidDepthStrings") String invalidDepth
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("ast-test");
        try {
            ByteArrayOutputStream errOut = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errOut));

            try {
                CommandLine cmd = new CommandLine(new AstSearchCommand());
                int exitCode = cmd.execute("myTerm", tempDir.toString(), invalidDepth);

                assertNotEquals(0, exitCode,
                    "Invalid depth '" + invalidDepth + "' must produce non-zero exit code");
            } finally {
                System.setErr(originalErr);
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Property(tries = 50)
    void negativeIntegerDepthIsRejected(
            @ForAll @IntRange(min = Integer.MIN_VALUE, max = 0) int negativeDepth
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("ast-test");
        try {
            ByteArrayOutputStream errOut = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errOut));

            try {
                CommandLine cmd = new CommandLine(new AstSearchCommand());
                int exitCode = cmd.execute("myTerm", tempDir.toString(), String.valueOf(negativeDepth));

                assertNotEquals(0, exitCode,
                    "Negative depth " + negativeDepth + " must produce non-zero exit code");
            } finally {
                System.setErr(originalErr);
            }
        } finally {
            deleteRecursively(tempDir);
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

    @Provide
    Arbitrary<String> invalidDepthStrings() {
        return Arbitraries.of("abc", "1.5", "-1", "0", "1e5", "null", "", " ", "two", "∞");
    }
}
