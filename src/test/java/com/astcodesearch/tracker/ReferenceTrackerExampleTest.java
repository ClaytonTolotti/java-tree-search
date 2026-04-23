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
 * Example-based tests for ReferenceTrackerImpl.
 * Validates: Requirements 4.1, 4.2
 */
class ReferenceTrackerExampleTest {

    @TempDir
    Path tempDir;

    /**
     * Reference with no usages → empty list returned.
     * Req 4.1
     */
    @Test
    void referenceWithNoUsagesReturnsEmptyList() throws IOException {
        Path javaFile = tempDir.resolve("Empty.java");
        Files.writeString(javaFile, "public class Empty { }");

        Reference ref = new Reference("nonExistentMethod", ReferenceType.METHOD, javaFile, 1, 0);
        ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());

        List<UsageNode> usages = tracker.findUsages(ref, List.of(javaFile), List.of());

        assertTrue(usages.isEmpty(), "Reference with no usages must return empty list");
    }

    /**
     * Usage via @Value Spring Boot → usage found.
     * Req 4.2
     */
    @Test
    void springValueUsageIsFound() throws IOException {
        String key = "app.timeout";
        Path javaFile = tempDir.resolve("MyBean.java");
        Files.writeString(javaFile,
            "import org.springframework.beans.factory.annotation.Value;\n" +
            "public class MyBean {\n" +
            "    @Value(\"${" + key + "}\")\n" +
            "    private int timeout;\n" +
            "}\n"
        );

        Reference ref = new Reference(key, ReferenceType.CONFIG_KEY, javaFile, 1, 0);
        ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());

        List<UsageNode> usages = tracker.findUsages(ref, List.of(javaFile), List.of());

        assertFalse(usages.isEmpty(), "@Value usage must be found");
    }

    /**
     * Usage via @ConfigProperty Quarkus → usage found.
     * Req 4.2
     */
    @Test
    void quarkusConfigPropertyUsageIsFound() throws IOException {
        String key = "quarkus.http.port";
        Path javaFile = tempDir.resolve("QuarkusBean.java");
        Files.writeString(javaFile,
            "import io.smallrye.config.ConfigProperty;\n" +
            "public class QuarkusBean {\n" +
            "    @ConfigProperty(name = \"" + key + "\")\n" +
            "    int httpPort;\n" +
            "}\n"
        );

        Reference ref = new Reference(key, ReferenceType.CONFIG_KEY, javaFile, 1, 0);
        ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());

        List<UsageNode> usages = tracker.findUsages(ref, List.of(javaFile), List.of());

        assertFalse(usages.isEmpty(), "@ConfigProperty usage must be found");
    }
}
