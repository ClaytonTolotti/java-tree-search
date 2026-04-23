package com.javatreesearch.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example-based tests for AstSearchCommand CLI.
 * Validates: Requirements 1.1, 1.4, 1.6, 1.7
 */
class AstSearchCommandExampleTest {

    @TempDir
    Path tempDir;

    /**
     * Missing required parameter (searchTerm) → exit code != 0.
     * Req 1.1, 1.6
     */
    @Test
    void missingSearchTermProducesNonZeroExitCode() {
        CommandLine cmd = new CommandLine(new AstSearchCommand());
        int exitCode = cmd.execute(); // no args
        assertNotEquals(0, exitCode, "Missing searchTerm must produce non-zero exit code");
    }

    /**
     * Default depth (5) when not provided.
     * Req 1.4
     */
    @Test
    void defaultDepthIsFiveWhenNotProvided() throws Exception {
        Path javaFile = tempDir.resolve("Foo.java");
        Files.writeString(javaFile, "public class Foo { }");

        // Capture the command and verify it runs with depth 5 by default
        AstSearchCommand command = new AstSearchCommand();
        CommandLine cmd = new CommandLine(command);
        // Parse args without depth
        cmd.parseArgs("myTerm", tempDir.toString());

        assertEquals("5", command.depthStr,
            "Default depth must be '5' when not provided");
    }

    /**
     * Invalid directory → error message, exit code != 0.
     * Req 1.6, 1.7
     */
    @Test
    void invalidDirectoryProducesNonZeroExitCode() {
        ByteArrayOutputStream errOut = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errOut));

        try {
            CommandLine cmd = new CommandLine(new AstSearchCommand());
            int exitCode = cmd.execute("myTerm", "/nonexistent/path/xyz", "5");

            assertNotEquals(0, exitCode, "Invalid directory must produce non-zero exit code");
            String errOutput = errOut.toString();
            assertTrue(errOutput.contains("Error") || errOutput.contains("error") ||
                       errOutput.contains("does not exist"),
                "Error message must be printed for invalid directory");
        } finally {
            System.setErr(originalErr);
        }
    }

    /**
     * Non-integer depth → error message, exit code != 0.
     * Req 1.7
     */
    @Test
    void nonIntegerDepthProducesNonZeroExitCode() {
        ByteArrayOutputStream errOut = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errOut));

        try {
            CommandLine cmd = new CommandLine(new AstSearchCommand());
            int exitCode = cmd.execute("myTerm", tempDir.toString(), "notANumber");

            assertNotEquals(0, exitCode, "Non-integer depth must produce non-zero exit code");
        } finally {
            System.setErr(originalErr);
        }
    }

    /**
     * Zero depth → error message, exit code != 0.
     * Req 1.7
     */
    @Test
    void zeroDepthProducesNonZeroExitCode() {
        CommandLine cmd = new CommandLine(new AstSearchCommand());
        int exitCode = cmd.execute("myTerm", tempDir.toString(), "0");
        assertNotEquals(0, exitCode, "Zero depth must produce non-zero exit code");
    }
}
