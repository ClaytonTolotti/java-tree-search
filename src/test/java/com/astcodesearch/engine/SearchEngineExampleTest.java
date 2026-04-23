package com.javatreesearch.engine;

import com.javatreesearch.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example-based tests for SearchEngineImpl.
 * Validates: Requirements 3.4, 5.4, 6.2
 */
class SearchEngineExampleTest {

    @TempDir
    Path tempDir;

    /**
     * Search with no results → informative message, empty iterations, code 0.
     * Req 3.4
     */
    @Test
    void searchWithNoResultsReturnsEmptyIterations() throws IOException {
        Path javaFile = tempDir.resolve("Empty.java");
        Files.writeString(javaFile, "public class Empty { }");

        ByteArrayOutputStream errOut = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errOut));

        try {
            SearchEngineImpl engine = new SearchEngineImpl();
            SearchResult result = engine.execute("nonExistentTerm", tempDir, 5);

            assertTrue(result.iterations().isEmpty(),
                "No results should produce empty iterations list");
            String errOutput = errOut.toString();
            assertTrue(errOutput.contains("No results") || errOutput.contains("no results") ||
                       errOutput.contains("nonExistentTerm"),
                "Should print informative message when no results found");
        } finally {
            System.setErr(originalErr);
        }
    }

    /**
     * Reference with no usages → explicit indication in result.
     * Req 5.4
     */
    @Test
    void referenceWithNoUsagesHasEmptyUsagesList() throws IOException {
        Path javaFile = tempDir.resolve("Isolated.java");
        Files.writeString(javaFile,
            "public class Isolated {\n" +
            "  void isolatedMethod() { }\n" +
            "}\n"
        );

        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute("isolatedMethod", tempDir, 2);

        // Should find the method definition but with no usages
        boolean foundWithNoUsages = result.iterations().stream()
            .anyMatch(iter -> iter.sourceReference().name().equals("isolatedMethod")
                && iter.usages().isEmpty());
        assertTrue(foundWithNoUsages,
            "Isolated method should appear in results with empty usages list");
    }

    /**
     * Repository with > 1000 files → progress indicator on stderr.
     * Req 6.2
     * Note: This test creates 1001 minimal Java files to trigger the progress bar.
     * It may be slow; in CI you can skip it with a tag.
     */
    @Test
    void largeRepositoryShowsProgressIndicator() throws IOException {
        // Create 1001 minimal Java files
        for (int i = 0; i < 1001; i++) {
            Path file = tempDir.resolve("File" + i + ".java");
            Files.writeString(file, "public class File" + i + " { }");
        }

        ByteArrayOutputStream errOut = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errOut));

        try {
            SearchEngineImpl engine = new SearchEngineImpl();
            engine.execute("File0", tempDir, 1);

            String errOutput = errOut.toString();
            assertTrue(errOutput.contains("Scanning") || errOutput.contains("files"),
                "Large repository should show progress indicator on stderr");
        } finally {
            System.setErr(originalErr);
        }
    }
}
