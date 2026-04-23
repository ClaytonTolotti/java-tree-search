package com.javatreesearch.tracker;

// Feature: ast-code-search, Property 12: Rastreamento Encontra Todos os Usos de uma Referência

import com.javatreesearch.model.Reference;
import com.javatreesearch.model.ReferenceType;
import com.javatreesearch.model.UsageNode;
import com.javatreesearch.parser.AstParserImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ReferenceTracker completeness.
 * Validates: Requirement 4.1
 */
class ReferenceTrackerAllUsagesPropertiesTest {

    /**
     * Property 12: Rastreamento Encontra Todos os Usos de uma Referência
     *
     * For any reference and any set of Java files, the ReferenceTracker must find
     * all nodes that use the reference name — without omitting any usage present in the files.
     *
     * Validates: Requirement 4.1
     */
    @Property(tries = 100)
    void trackerFindsAllUsagesAcrossFiles(
            @ForAll @IntRange(min = 1, max = 4) int fileCount,
            @ForAll @IntRange(min = 1, max = 3) int usagesPerFile
    ) throws IOException {
        String refName = "targetMethod";
        Path dir = Files.createTempDirectory("ast-test");
        try {
            List<Path> javaFiles = new ArrayList<>();
            for (int f = 0; f < fileCount; f++) {
                StringBuilder sb = new StringBuilder("public class File" + f + " {\n");
                sb.append("  void caller() {\n");
                for (int u = 0; u < usagesPerFile; u++) {
                    sb.append("    ").append(refName).append("();\n");
                }
                sb.append("  }\n");
                sb.append("  void ").append(refName).append("() {}\n");
                sb.append("}\n");

                Path file = dir.resolve("File" + f + ".java");
                Files.writeString(file, sb.toString());
                javaFiles.add(file);
            }

            Reference ref = new Reference(refName, ReferenceType.METHOD, javaFiles.get(0), 1, 0);
            ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());

            List<UsageNode> usages = tracker.findUsages(ref, javaFiles, List.of());

            long methodCallUsages = usages.stream()
                .filter(u -> u.astNode() instanceof com.github.javaparser.ast.expr.MethodCallExpr)
                .count();

            assertTrue(methodCallUsages >= (long) fileCount * usagesPerFile,
                "Must find at least " + (fileCount * usagesPerFile) + " method call usages across " +
                fileCount + " files, found: " + methodCallUsages);
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
