package com.javatreesearch.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example-based tests for AstParserImpl error handling.
 * Validates: Requirements 2.6, 2.8
 */
class AstParserExampleTest {

    @TempDir
    Path tempDir;

    /**
     * Req 2.6: Java file with syntax error → AstSyntaxException thrown (caller logs warning and continues).
     */
    @Test
    void syntaxErrorFileThrowsAstSyntaxException() throws IOException {
        Path badFile = tempDir.resolve("Bad.java");
        Files.writeString(badFile, "this is not valid java @@@ !!!");

        AstParserImpl parser = new AstParserImpl();
        assertThrows(AstParserImpl.AstSyntaxException.class,
            () -> parser.getAst(badFile, "not"),
            "Syntax error should throw AstSyntaxException");
    }

    /**
     * Req 2.8: File unreadable due to permission error → AstIoException thrown (caller logs warning and continues).
     * Note: This test is skipped on non-POSIX systems (e.g., Windows).
     */
    @Test
    void unreadableFileThrowsAstIoException() throws IOException {
        // Skip on non-POSIX systems
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return;
        }

        Path restrictedFile = tempDir.resolve("Restricted.java");
        Files.writeString(restrictedFile, "public class Restricted {}");
        // Remove read permission
        Files.setPosixFilePermissions(restrictedFile,
            PosixFilePermissions.fromString("---------"));

        AstParserImpl parser = new AstParserImpl();
        assertThrows(AstParserImpl.AstIoException.class,
            () -> parser.getAst(restrictedFile, "Restricted"),
            "Unreadable file should throw AstIoException");

        // Restore permissions for cleanup
        Files.setPosixFilePermissions(restrictedFile,
            PosixFilePermissions.fromString("rw-r--r--"));
    }

    /**
     * Valid Java file returns a present CompilationUnit.
     */
    @Test
    void validJavaFileReturnsPresentAst() throws IOException {
        Path validFile = tempDir.resolve("Valid.java");
        Files.writeString(validFile, "public class Valid { void myMethod() {} }");

        AstParserImpl parser = new AstParserImpl();
        Optional<?> result = parser.getAst(validFile, "myMethod");

        assertTrue(result.isPresent(), "Valid Java file should return a present AST");
    }
}
