package com.javatreesearch.engine;

// Feature: ast-code-search, Property 14: Terminação Garantida na Presença de Ciclos

import com.javatreesearch.model.SearchResult;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for SearchEngine termination with cyclic references.
 * Validates: Requirement 4.7
 */
class SearchEngineTerminationPropertiesTest {

    // @TempDir is only used by @Test methods below; @Property methods create their own temp dirs
    @TempDir
    Path tempDir;

    /**
     * Property 14: Terminação Garantida na Presença de Ciclos
     *
     * For any reference graph containing cycles (references that reference each other),
     * the tracking must terminate in finite time without entering an infinite loop.
     *
     * Validates: Requirement 4.7
     */
    @Property(tries = 20)
    void terminatesWithCyclicReferences(
            @ForAll @IntRange(min = 2, max = 4) int cycleSize,
            @ForAll @IntRange(min = 1, max = 3) int maxDepth
    ) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Path dir = Files.createTempDirectory("ast-test");
        try {
            StringBuilder sb = new StringBuilder("public class Cycle {\n");
            for (int i = 0; i < cycleSize; i++) {
                int next = (i + 1) % cycleSize;
                sb.append("  void method").append(i).append("() {\n");
                sb.append("    method").append(next).append("();\n");
                sb.append("  }\n");
            }
            sb.append("}\n");

            Files.writeString(dir.resolve("Cycle.java"), sb.toString());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<SearchResult> future = executor.submit(() -> {
                SearchEngineImpl engine = new SearchEngineImpl();
                return engine.execute("method0", dir, maxDepth);
            });

            SearchResult result = future.get(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertNotNull(result, "Search must complete and return a result");
            result.iterations().forEach(iter ->
                assertTrue(iter.depth() <= maxDepth,
                    "Depth must not exceed maxDepth even with cycles")
            );
        } finally {
            deleteRecursively(dir);
        }
    }

    /**
     * Direct mutual recursion: methodA calls methodB, methodB calls methodA.
     */
    @Test
    void mutualRecursionTerminates() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Path javaFile = tempDir.resolve("Mutual.java");
        Files.writeString(javaFile,
            "public class Mutual {\n" +
            "  void methodA() { methodB(); }\n" +
            "  void methodB() { methodA(); }\n" +
            "}\n"
        );

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<SearchResult> future = executor.submit(() -> {
            SearchEngineImpl engine = new SearchEngineImpl();
            return engine.execute("methodA", tempDir, 5);
        });

        SearchResult result = future.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertNotNull(result, "Mutual recursion search must terminate");
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
