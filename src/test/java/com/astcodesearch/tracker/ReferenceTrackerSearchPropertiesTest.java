package com.javatreesearch.tracker;

// Feature: ast-code-search, Property 10: Busca Encontra Todas as Ocorrências Exatas

import com.javatreesearch.model.Reference;
import com.javatreesearch.model.ReferenceType;
import com.javatreesearch.model.UsageNode;
import com.javatreesearch.parser.AstParserImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotBlank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ReferenceTracker search correctness.
 * Validates: Requirement 3.1
 */
class ReferenceTrackerSearchPropertiesTest {

    /**
     * Property 10: Busca Encontra Todas as Ocorrências Exatas
     *
     * For any set of Java files and any search term, the tracker must find all exact
     * occurrences of the term in AST nodes — no false negatives (missed occurrences)
     * and no false positives (occurrences that don't contain the exact term).
     *
     * Validates: Requirement 3.1
     */
    @Property(tries = 100)
    void trackerFindsAllExactOccurrences(
            @ForAll @NotBlank String identifier,
            @ForAll @IntRange(min = 1, max = 5) int usageCount
    ) throws IOException {
        String sanitized = identifier.replaceAll("[^a-zA-Z]", "X");
        if (sanitized.isEmpty()) sanitized = "MyVar";
        String varName = sanitized;

        StringBuilder sb = new StringBuilder("public class TestClass {\n");
        sb.append("  int ").append(varName).append(" = 0;\n");
        sb.append("  void method() {\n");
        for (int i = 0; i < usageCount; i++) {
            sb.append("    int tmp").append(i).append(" = ").append(varName).append(";\n");
        }
        sb.append("  }\n}\n");

        Path dir = Files.createTempDirectory("ast-test");
        try {
            Path javaFile = dir.resolve("TestClass.java");
            Files.writeString(javaFile, sb.toString());

            Reference ref = new Reference(varName, ReferenceType.VARIABLE, javaFile, 2, 2);
            ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());

            List<UsageNode> usages = tracker.findUsages(ref, List.of(javaFile), List.of());

            for (UsageNode usage : usages) {
                assertTrue(usage.astNode().toString().contains(varName),
                    "All found usages must contain the exact term '" + varName + "'");
            }

            long nameExprUsages = usages.stream()
                .filter(u -> u.astNode().toString().equals(varName))
                .count();
            assertTrue(nameExprUsages >= usageCount,
                "Must find at least " + usageCount + " exact NameExpr usages, found: " + nameExprUsages);
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
