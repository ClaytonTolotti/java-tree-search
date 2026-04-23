package com.javatreesearch.tracker;

// Feature: ast-code-search, Property 13: Rastreamento de Config-Key via Anotações

import com.javatreesearch.model.Reference;
import com.javatreesearch.model.ReferenceType;
import com.javatreesearch.model.UsageNode;
import com.javatreesearch.parser.AstParserImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ReferenceTracker annotation-based config key tracking.
 * Validates: Requirement 4.2
 */
class ReferenceTrackerAnnotationPropertiesTest {

    // @TempDir is only used by @Test methods; @Property methods create their own temp dirs
    @TempDir
    Path tempDir;

    /**
     * Property 13: Rastreamento de Config-Key via Anotações
     *
     * For any config key and any set of Java files, the ReferenceTracker must find
     * all fields annotated with @Value("${key}"), @Value("${key:default}"),
     * @ConfigProperty(name = "key"), or @ConfigProperty(name = "key", defaultValue = "...")
     * as usages of the key.
     *
     * Validates: Requirement 4.2
     */
    @Property(tries = 50)
    void springValueAnnotationIsFound(
            @ForAll @NotBlank String keyPart
    ) throws IOException {
        String sanitized = keyPart.replaceAll("[^a-zA-Z0-9.]", "x");
        if (sanitized.isEmpty()) sanitized = "mykey";
        String key = "app." + sanitized;

        String javaContent =
            "import org.springframework.beans.factory.annotation.Value;\n" +
            "public class SpringService {\n" +
            "    @Value(\"${" + key + "}\")\n" +
            "    private String myField;\n" +
            "}\n";

        Path dir = Files.createTempDirectory("ast-test");
        try {
            Path javaFile = dir.resolve("SpringService.java");
            Files.writeString(javaFile, javaContent);

            Reference ref = new Reference(key, ReferenceType.CONFIG_KEY, javaFile, 1, 0);
            ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());

            List<UsageNode> usages = tracker.findUsages(ref, List.of(javaFile), List.of());

            assertFalse(usages.isEmpty(),
                "@Value(\"${" + key + "}\") must be found as a usage of config key '" + key + "'");
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void springValueWithDefaultIsFound() throws IOException {
        String key = "server.port";
        String javaContent =
            "import org.springframework.beans.factory.annotation.Value;\n" +
            "public class SpringService {\n" +
            "    @Value(\"${" + key + ":8080}\")\n" +
            "    private int port;\n" +
            "}\n";

        Path javaFile = tempDir.resolve("SpringService.java");
        Files.writeString(javaFile, javaContent);

        Reference ref = new Reference(key, ReferenceType.CONFIG_KEY, javaFile, 1, 0);
        ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());

        List<UsageNode> usages = tracker.findUsages(ref, List.of(javaFile), List.of());

        assertFalse(usages.isEmpty(), "@Value(\"${" + key + ":8080}\") must be found as a usage");
    }

    @Test
    void quarkusConfigPropertyIsFound() throws IOException {
        String key = "quarkus.datasource.url";
        String javaContent =
            "import io.smallrye.config.ConfigProperty;\n" +
            "public class QuarkusService {\n" +
            "    @ConfigProperty(name = \"" + key + "\")\n" +
            "    String datasourceUrl;\n" +
            "}\n";

        Path javaFile = tempDir.resolve("QuarkusService.java");
        Files.writeString(javaFile, javaContent);

        Reference ref = new Reference(key, ReferenceType.CONFIG_KEY, javaFile, 1, 0);
        ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());

        List<UsageNode> usages = tracker.findUsages(ref, List.of(javaFile), List.of());

        assertFalse(usages.isEmpty(), "@ConfigProperty(name = \"" + key + "\") must be found as a usage");
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
