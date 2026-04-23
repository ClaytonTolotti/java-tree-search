package com.javatreesearch.integration;

import com.javatreesearch.parser.AstParserImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for nested directory structure and depth limit.
 * Validates: Requirements 2.1, 4.5, 4.6
 */
class DirectoryDepthIntegrationTest {

    @TempDir
    Path repoRoot;

    /**
     * Files nested beyond the directory depth limit must not be listed.
     * Req 2.1, 4.5, 4.6
     */
    @Test
    void directoryDepthLimitIsRespected() throws IOException {
        // Create files at various depths
        // depth 1: repoRoot/File1.java
        Path file1 = repoRoot.resolve("File1.java");
        Files.writeString(file1, "public class File1 { }");

        // depth 2: repoRoot/level1/File2.java
        Path level1 = repoRoot.resolve("level1");
        Files.createDirectories(level1);
        Path file2 = level1.resolve("File2.java");
        Files.writeString(file2, "public class File2 { }");

        // depth 3: repoRoot/level1/level2/File3.java
        Path level2 = level1.resolve("level2");
        Files.createDirectories(level2);
        Path file3 = level2.resolve("File3.java");
        Files.writeString(file3, "public class File3 { }");

        // depth 4: repoRoot/level1/level2/level3/File4.java
        Path level3 = level2.resolve("level3");
        Files.createDirectories(level3);
        Path file4 = level3.resolve("File4.java");
        Files.writeString(file4, "public class File4 { }");

        // With maxDirectoryDepth=2, only files at depth 1 and 2 should be listed
        AstParserImpl parser = new AstParserImpl(2);
        List<Path> files = parser.listJavaFiles(repoRoot);

        assertTrue(files.contains(file1), "File at depth 1 must be listed");
        assertTrue(files.contains(file2), "File at depth 2 must be listed");
        assertFalse(files.contains(file3), "File at depth 3 must NOT be listed with maxDepth=2");
        assertFalse(files.contains(file4), "File at depth 4 must NOT be listed with maxDepth=2");
    }

    /**
     * With unlimited depth, all files are listed regardless of nesting.
     * Req 2.1
     */
    @Test
    void unlimitedDepthListsAllFiles() throws IOException {
        Path deep = repoRoot.resolve("a/b/c/d/e");
        Files.createDirectories(deep);
        Path deepFile = deep.resolve("Deep.java");
        Files.writeString(deepFile, "public class Deep { }");

        AstParserImpl parser = new AstParserImpl(); // default: Integer.MAX_VALUE
        List<Path> files = parser.listJavaFiles(repoRoot);

        assertTrue(files.contains(deepFile),
            "Deeply nested file must be listed with unlimited depth");
    }
}
