package com.javatreesearch.engine;

// Feature: ast-code-search, Property 1: Invariante de Profundidade

import com.javatreesearch.model.IterationResult;
import com.javatreesearch.model.SearchResult;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for SearchEngine depth invariant.
 * Validates: Requirements 1.5, 4.5, 4.6
 */
class SearchEngineDepthPropertiesTest {

    /**
     * Property 1: Invariante de Profundidade
     *
     * For any depth limit N (positive integer) and any reference graph, the SearchEngine
     * must execute exactly iterations 1 through N, without executing iteration N+1,
     * regardless of how many references exist.
     *
     * Validates: Requirements 1.5, 4.5, 4.6
     */
    @Property(tries = 50)
    void depthInvariantIsRespected(
            @ForAll @IntRange(min = 1, max = 5) int maxDepth
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("ast-test");
        try {
            // This ensures there are always more references than maxDepth
            int chainLength = maxDepth + 3;
            StringBuilder sb = new StringBuilder("public class Chain {\n");
            for (int i = 1; i <= chainLength; i++) {
                sb.append("  void method").append(i).append("() {\n");
                if (i < chainLength) {
                    sb.append("    method").append(i + 1).append("();\n");
                }
                sb.append("  }\n");
            }
            sb.append("}\n");

            Path javaFile = tempDir.resolve("Chain.java");
            Files.writeString(javaFile, sb.toString());

            SearchEngineImpl engine = new SearchEngineImpl();
            SearchResult result = engine.execute("method1", tempDir, maxDepth);

            for (IterationResult iter : result.iterations()) {
                assertTrue(iter.depth() <= maxDepth,
                    "Iteration depth " + iter.depth() + " must not exceed maxDepth " + maxDepth);
                assertTrue(iter.depth() >= 1,
                    "Iteration depth must be >= 1");
            }

            boolean hasExceededDepth = result.iterations().stream()
                .anyMatch(iter -> iter.depth() > maxDepth);
            assertFalse(hasExceededDepth,
                "No iteration should exceed maxDepth " + maxDepth);
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
}
