package com.javatreesearch.tracker;

// Feature: lombok-reference-support, Property 11: Não-regressão para referências sem Lombok

import com.javatreesearch.model.Reference;
import com.javatreesearch.model.ReferenceType;
import com.javatreesearch.model.UsageNode;
import com.javatreesearch.parser.AstParserImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based non-regression tests for ReferenceTracker with plain (non-Lombok) classes.
 *
 * Validates: Requirements 3.4, 4.3
 */
class ReferenceTrackerNonRegressionPropertiesTest {

    /**
     * Property 11: Não-regressão para referências sem Lombok
     *
     * For any Reference whose source field belongs to a class without any LombokAnnotation,
     * the result of findUsages SHALL be identical to the result produced before the
     * implementation of this feature:
     *   1. The result contains the expected number of NameExpr usages.
     *   2. No extra usages are added (the Lombok path returns early when no annotations are detected).
     *   3. All found usages contain the exact field name.
     *
     * Validates: Requirements 3.4, 4.3
     */
    @Property(tries = 50)
    void nonLombokClassProducesUnchangedResults(
            @ForAll("validFieldNames") String fieldName,
            @ForAll @IntRange(min = 1, max = 5) int usageCount
    ) throws IOException {
        // Build a plain Java class with NO Lombok annotations
        StringBuilder sb = new StringBuilder();
        sb.append("public class PlainClass {\n");
        sb.append("    private String ").append(fieldName).append(";\n");
        sb.append("    void method() {\n");
        for (int i = 0; i < usageCount; i++) {
            sb.append("        String tmp").append(i).append(" = ").append(fieldName).append(";\n");
        }
        sb.append("    }\n");
        sb.append("}\n");

        Path tempDir = Files.createTempDirectory("non-regression-test");
        try {
            Path javaFile = tempDir.resolve("PlainClass.java");
            Files.writeString(javaFile, sb.toString());

            // Reference points to the field in the plain class (line 2)
            Reference reference = new Reference(fieldName, ReferenceType.VARIABLE, javaFile, 2, 19);

            ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());
            List<UsageNode> usages = tracker.findUsages(reference, List.of(javaFile), List.of());

            // 1. The result contains the expected number of NameExpr usages (same as before Lombok support)
            long nameExprUsages = usages.stream()
                .filter(u -> u.astNode().toString().equals(fieldName))
                .count();
            assertTrue(nameExprUsages >= usageCount,
                "Must find at least " + usageCount + " exact NameExpr usages for field '" +
                fieldName + "', found: " + nameExprUsages);

            // 2. No extra usages are added by the Lombok path (which returns early when no annotations detected)
            // The total usages should only be the NameExpr usages in the method body
            // (the field declaration itself is not a NameExpr usage, so total == usageCount)
            assertEquals(usageCount, usages.size(),
                "Total usages must equal exactly the number of NameExpr usages in the method body (" +
                usageCount + "), but found " + usages.size() +
                " — Lombok path must not add extra usages for plain classes");

            // 3. All found usages contain the exact field name
            for (UsageNode usage : usages) {
                assertTrue(usage.astNode().toString().contains(fieldName),
                    "Every usage must contain the exact field name '" + fieldName +
                    "', but found: '" + usage.astNode().toString() + "'");
            }

        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Provide
    Arbitrary<String> validFieldNames() {
        // Generate valid Java field names: start with lowercase letter, followed by letters only
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(1)
            .ofMaxLength(15);
    }

    private void deleteRecursively(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                      .forEach(p -> {
                          try {
                              Files.delete(p);
                          } catch (IOException ignored) {}
                      });
            }
        }
    }
}
