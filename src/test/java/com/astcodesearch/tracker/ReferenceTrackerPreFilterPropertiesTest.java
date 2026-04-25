package com.javatreesearch.tracker;

// Feature: lombok-reference-support, Property 12: Pré-filtro aplicado a métodos Lombok

import com.github.javaparser.ast.CompilationUnit;
import com.javatreesearch.model.Reference;
import com.javatreesearch.model.ReferenceType;
import com.javatreesearch.model.UsageNode;
import com.javatreesearch.parser.AstParser;
import com.javatreesearch.parser.AstParserImpl;
import net.jqwik.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ReferenceTracker pre-filter behavior on Lombok-derived methods.
 *
 * Validates: Requirements 5.1, 5.2
 */
class ReferenceTrackerPreFilterPropertiesTest {

    /**
     * Tracking AstParser that delegates to a real AstParserImpl but records which files
     * were queried with which terms. This allows us to verify that the pre-filter prevents
     * getAst from being called on files that don't contain the derived method name.
     */
    static class TrackingAstParser implements AstParser {
        private final AstParser delegate = new AstParserImpl();
        private final Map<Path, Set<String>> parsedFilesWithTerms = new HashMap<>();

        @Override
        public Optional<CompilationUnit> getAst(Path file, String term) {
            parsedFilesWithTerms.computeIfAbsent(file, k -> new HashSet<>()).add(term);
            return delegate.getAst(file, term);
        }

        @Override
        public List<Path> listJavaFiles(Path rootDir) {
            return delegate.listJavaFiles(rootDir);
        }

        public boolean wasFileQueriedWithTerm(Path file, String term) {
            return parsedFilesWithTerms.getOrDefault(file, Set.of()).contains(term);
        }
    }

    /**
     * Property 12: Pré-filtro aplicado a métodos Lombok
     *
     * For any Java file whose textual content does not contain the name of a LombokDerivedMethod,
     * the ReferenceTracker SHALL NOT invoke AstParser.getAst for that file when searching for
     * that derived method.
     *
     * Validates: Requirements 5.1, 5.2
     */
    @Property(tries = 50)
    void preFilterPreventsAstParsingOfFilesNotContainingDerivedMethodName(
            @ForAll("validFieldNames") String fieldName
    ) throws IOException {
        // Derive the getter name that Lombok would generate for this field
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        // Source class: annotated with @Data, contains the field
        String sourceClassContent = String.format(
            "import lombok.Data;\n" +
            "@Data\n" +
            "public class Config {\n" +
            "    private String %s;\n" +
            "}\n",
            fieldName
        );

        // Consumer file: DOES contain the getter name (so it should be parsed)
        String consumerClassContent = String.format(
            "public class Consumer {\n" +
            "    void use(Config c) {\n" +
            "        c.%s();\n" +
            "    }\n" +
            "}\n",
            getterName
        );

        // Decoy file: does NOT contain the getter name — only unrelated content
        // We use a class name and field name that are guaranteed to differ from the getter
        String decoyClassContent =
            "public class Decoy {\n" +
            "    private int unrelatedField;\n" +
            "    public void doSomethingUnrelated() {\n" +
            "        int x = unrelatedField + 1;\n" +
            "    }\n" +
            "}\n";

        // Sanity check: the decoy file must NOT contain the getter name
        assertFalse(decoyClassContent.contains(getterName),
            "Test setup error: decoy file should not contain getter name '" + getterName + "'");

        Path tempDir = Files.createTempDirectory("prefilter-test");
        try {
            Path sourceFile = tempDir.resolve("Config.java");
            Path consumerFile = tempDir.resolve("Consumer.java");
            Path decoyFile = tempDir.resolve("Decoy.java");

            Files.writeString(sourceFile, sourceClassContent);
            Files.writeString(consumerFile, consumerClassContent);
            Files.writeString(decoyFile, decoyClassContent);

            // Reference points to the field in the source file
            Reference reference = new Reference(fieldName, ReferenceType.VARIABLE, sourceFile, 4, 19);

            TrackingAstParser trackingParser = new TrackingAstParser();
            ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(trackingParser);

            List<UsageNode> usages = tracker.findUsages(
                reference,
                List.of(sourceFile, consumerFile, decoyFile),
                List.of()
            );

            // The decoy file must NOT have been queried with the getter name as the term.
            // This verifies that the pre-filter (String.contains check) prevented getAst
            // from being called on the decoy file for the derived method name.
            assertFalse(
                trackingParser.wasFileQueriedWithTerm(decoyFile, getterName),
                "Pre-filter violation: AstParser.getAst was called for decoy file '" + decoyFile +
                "' with term '" + getterName + "', but the file does not contain that term. " +
                "The pre-filter should have skipped this file."
            );

            // Sanity check: the consumer file SHOULD have been queried with the getter name
            // (it contains the getter call, so it should be parsed)
            assertTrue(
                trackingParser.wasFileQueriedWithTerm(consumerFile, getterName),
                "Expected AstParser.getAst to be called for consumer file with term '" + getterName +
                "', but it was not. Consumer file contains the getter call."
            );

            // Sanity check: at least one usage should be found (the getter call in the consumer)
            boolean hasGetterUsage = usages.stream()
                .anyMatch(u -> u.astNode().toString().contains(getterName));
            assertTrue(hasGetterUsage,
                "Expected to find getter usage '" + getterName + "' in consumer file, but none found. " +
                "Usages: " + usages.stream().map(u -> u.astNode().toString()).toList());

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
                stream.sorted(Comparator.reverseOrder())
                      .forEach(p -> {
                          try {
                              Files.delete(p);
                          } catch (IOException ignored) {}
                      });
            }
        }
    }
}
