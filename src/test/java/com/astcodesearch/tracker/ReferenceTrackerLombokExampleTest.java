package com.javatreesearch.tracker;

import com.javatreesearch.model.Reference;
import com.javatreesearch.model.ReferenceType;
import com.javatreesearch.model.UsageNode;
import com.javatreesearch.parser.AstParserImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example-based tests for ReferenceTrackerImpl with Lombok support.
 *
 * Validates: Requirements 3.1, 3.2, 3.4, 3.5
 */
class ReferenceTrackerLombokExampleTest {

    @TempDir
    Path tempDir;

    /**
     * Test 1: Integration with a real Java file containing @Data and calls to the getter.
     *
     * Source:   @Data public class Config { private String pathDownload; }
     * Consumer: public class Service { void use(Config c) { c.getPathDownload(); } }
     *
     * Assert: findUsages for "pathDownload" finds getPathDownload() in the consumer file.
     *
     * Validates: Requirements 3.1, 3.2, 3.5
     */
    @Test
    void dataAnnotationGetterIsFoundInConsumer() throws IOException {
        Path sourceFile = tempDir.resolve("Config.java");
        Files.writeString(sourceFile,
            "import lombok.Data;\n" +
            "@Data\n" +
            "public class Config {\n" +
            "    private String pathDownload;\n" +
            "}\n"
        );

        Path consumerFile = tempDir.resolve("Service.java");
        Files.writeString(consumerFile,
            "public class Service {\n" +
            "    void use(Config c) {\n" +
            "        c.getPathDownload();\n" +
            "    }\n" +
            "}\n"
        );

        // Reference points to the field "pathDownload" in the source file (line 4)
        Reference reference = new Reference("pathDownload", ReferenceType.VARIABLE, sourceFile, 4, 19);

        ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());
        List<UsageNode> usages = tracker.findUsages(reference, List.of(sourceFile, consumerFile), List.of());

        // Assert that at least one UsageNode contains "getPathDownload"
        boolean hasGetterUsage = usages.stream()
            .anyMatch(u -> u.astNode().toString().contains("getPathDownload"));

        assertTrue(hasGetterUsage,
            "findUsages must include a UsageNode for getPathDownload() in the consumer file. " +
            "Found usages: " + usages.stream().map(u -> u.astNode().toString()).toList());

        // Assert that the getter usage is in the consumer file
        UsageNode getterUsage = usages.stream()
            .filter(u -> u.astNode().toString().contains("getPathDownload"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No getPathDownload usage found"));

        assertEquals(consumerFile, getterUsage.file(),
            "getPathDownload() usage must be located in the consumer file");

        assertTrue(getterUsage.line() > 0,
            "getPathDownload() usage must have a valid line number (> 0), got: " + getterUsage.line());
    }

    /**
     * Test 2: Boolean field with isXxx() being called.
     *
     * Source:   @Data public class Status { private boolean active; }
     * Consumer: public class Checker { void check(Status s) { s.isActive(); } }
     *
     * Assert: findUsages for "active" finds isActive() in the consumer file.
     *
     * Validates: Requirements 3.1, 3.2, 3.5
     */
    @Test
    void booleanFieldIsGetterIsFoundInConsumer() throws IOException {
        Path sourceFile = tempDir.resolve("Status.java");
        Files.writeString(sourceFile,
            "import lombok.Data;\n" +
            "@Data\n" +
            "public class Status {\n" +
            "    private boolean active;\n" +
            "}\n"
        );

        Path consumerFile = tempDir.resolve("Checker.java");
        Files.writeString(consumerFile,
            "public class Checker {\n" +
            "    void check(Status s) {\n" +
            "        s.isActive();\n" +
            "    }\n" +
            "}\n"
        );

        // Reference points to the field "active" in the source file (line 4)
        Reference reference = new Reference("active", ReferenceType.VARIABLE, sourceFile, 4, 20);

        ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());
        List<UsageNode> usages = tracker.findUsages(reference, List.of(sourceFile, consumerFile), List.of());

        // Assert that at least one UsageNode contains "isActive"
        boolean hasIsGetterUsage = usages.stream()
            .anyMatch(u -> u.astNode().toString().contains("isActive"));

        assertTrue(hasIsGetterUsage,
            "findUsages must include a UsageNode for isActive() in the consumer file. " +
            "Found usages: " + usages.stream().map(u -> u.astNode().toString()).toList());

        // Assert that the isActive usage is in the consumer file
        UsageNode isGetterUsage = usages.stream()
            .filter(u -> u.astNode().toString().contains("isActive"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No isActive usage found"));

        assertEquals(consumerFile, isGetterUsage.file(),
            "isActive() usage must be located in the consumer file");

        assertTrue(isGetterUsage.line() > 0,
            "isActive() usage must have a valid line number (> 0), got: " + isGetterUsage.line());
    }

    /**
     * Test 3: Without Lombok — result identical to previous behavior.
     *
     * Source:   public class Plain { private String name; }
     * Consumer: public class User { String name; void use() { System.out.println(name); } }
     *
     * The consumer references "name" as a standalone NameExpr (not a field access like p.name,
     * which JavaParser parses as FieldAccessExpr rather than NameExpr).
     *
     * Assert: findUsages for "name" finds the "name" NameExpr in the consumer file,
     *         but does NOT find any getName() call (since there's no Lombok).
     *
     * Validates: Requirements 3.4
     */
    @Test
    void withoutLombokOnlyDirectNameExprIsFound() throws IOException {
        Path sourceFile = tempDir.resolve("Plain.java");
        Files.writeString(sourceFile,
            "public class Plain {\n" +
            "    private String name;\n" +
            "}\n"
        );

        // The consumer uses "name" as a standalone NameExpr (direct variable reference),
        // which is what the tracker's NameExpr search finds.
        Path consumerFile = tempDir.resolve("User.java");
        Files.writeString(consumerFile,
            "public class User {\n" +
            "    String name;\n" +
            "    void use() {\n" +
            "        System.out.println(name);\n" +
            "    }\n" +
            "}\n"
        );

        // Reference points to the field "name" in the source file (line 2)
        Reference reference = new Reference("name", ReferenceType.VARIABLE, sourceFile, 2, 19);

        ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());
        List<UsageNode> usages = tracker.findUsages(reference, List.of(sourceFile, consumerFile), List.of());

        // Assert that the direct "name" NameExpr is found in the consumer file
        boolean hasNameExprUsage = usages.stream()
            .anyMatch(u -> u.file().equals(consumerFile) &&
                          u.astNode().toString().equals("name"));

        assertTrue(hasNameExprUsage,
            "findUsages must include a NameExpr 'name' in the consumer file. " +
            "Found usages: " + usages.stream()
                .map(u -> u.file().getFileName() + ":" + u.line() + " -> " + u.astNode())
                .toList());

        // Assert that NO getName() call is found (no Lombok, so no getter should be derived)
        boolean hasGetterUsage = usages.stream()
            .anyMatch(u -> u.astNode().toString().contains("getName"));

        assertFalse(hasGetterUsage,
            "findUsages must NOT include any getName() call when there is no Lombok annotation. " +
            "Found usages: " + usages.stream().map(u -> u.astNode().toString()).toList());
    }
}
