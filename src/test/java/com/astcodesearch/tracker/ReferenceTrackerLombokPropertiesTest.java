package com.javatreesearch.tracker;

// Feature: lombok-reference-support, Property 9: Rastreamento de métodos Lombok derivados

import com.javatreesearch.model.Reference;
import com.javatreesearch.model.ReferenceType;
import com.javatreesearch.model.UsageNode;
import com.javatreesearch.parser.AstParserImpl;
import net.jqwik.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ReferenceTracker Lombok method tracking.
 *
 * Validates: Requirements 3.1, 3.2, 3.5
 */
class ReferenceTrackerLombokPropertiesTest {

    /**
     * Property 9: Rastreamento de métodos Lombok derivados
     *
     * For any Reference whose source field belongs to a class with LombokAnnotation,
     * the ReferenceTracker SHALL include in findUsages results all MethodCallExprs whose
     * names match the LombokDerivedMethods derived for that field, with correct file,
     * line, and column.
     *
     * Validates: Requirements 3.1, 3.2, 3.5
     */
    @Property(tries = 50)
    void lombokDerivedMethodsAreTracked(
            @ForAll("validFieldNames") String fieldName
    ) throws IOException {
        // Capitalize the field name for the getter
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        // Build the source class with @Data annotation
        String sourceClassContent = String.format(
            "import lombok.Data;\n" +
            "@Data\n" +
            "public class Config {\n" +
            "    private String %s;\n" +
            "}\n",
            fieldName
        );

        // Build the consumer class that calls the getter
        String consumerClassContent = String.format(
            "public class Consumer {\n" +
            "    void use(Config c) {\n" +
            "        c.%s();\n" +
            "    }\n" +
            "}\n",
            getterName
        );

        Path tempDir = Files.createTempDirectory("lombok-tracker-test");
        try {
            Path sourceFile = tempDir.resolve("Config.java");
            Path consumerFile = tempDir.resolve("Consumer.java");

            Files.writeString(sourceFile, sourceClassContent);
            Files.writeString(consumerFile, consumerClassContent);

            // Reference points to the field in the source file (line 4, column 19 for "private String fieldName;")
            Reference reference = new Reference(fieldName, ReferenceType.VARIABLE, sourceFile, 4, 19);

            ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());
            List<UsageNode> usages = tracker.findUsages(reference, List.of(sourceFile, consumerFile), List.of());

            // Assert that at least one UsageNode contains the getter name
            boolean hasGetterUsage = usages.stream()
                .anyMatch(u -> u.astNode().toString().contains(getterName));

            assertTrue(hasGetterUsage,
                "findUsages must include a UsageNode containing getter '" + getterName +
                "' for field '" + fieldName + "'. Found usages: " + usages.stream()
                    .map(u -> u.astNode().toString())
                    .toList());

            // Assert that the UsageNode with the getter is in the consumer file with a valid line number
            UsageNode getterUsage = usages.stream()
                .filter(u -> u.astNode().toString().contains(getterName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No getter usage found"));

            assertEquals(consumerFile, getterUsage.file(),
                "Getter usage must be in the consumer file");

            assertTrue(getterUsage.line() > 0,
                "Getter usage must have a valid line number (> 0), got: " + getterUsage.line());

        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Provide
    Arbitrary<String> validFieldNames() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(1)
            .ofMaxLength(20);
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
