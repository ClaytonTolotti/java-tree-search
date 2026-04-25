package com.javatreesearch.integration;

import com.javatreesearch.engine.SearchEngineImpl;
import com.javatreesearch.model.IterationResult;
import com.javatreesearch.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * End-to-end integration tests for Lombok support in the search engine.
 * Validates: Requirements 4.1, 4.2, 4.3, 4.4
 */
class SearchEngineLombokIntegrationTest {

    @TempDir
    Path repoRoot;

    /**
     * Test 1: Spring Boot config class with @Data + @Value finds getter usage.
     *
     * A field `pathDownload` annotated with @Value("${app.path-download}") and @Data
     * on the class should cause the engine to find usages of `getPathDownload()` in
     * other service classes.
     *
     * Validates: Requirements 4.1, 4.2, 4.4
     */
    @Test
    void springBootDataAnnotationFindsGetterUsage() throws IOException {
        // Create fixture: application.properties
        Path resourcesDir = repoRoot.resolve("src/main/resources");
        Files.createDirectories(resourcesDir);
        Path propsFile = resourcesDir.resolve("application.properties");
        Files.writeString(propsFile, "app.path-download=/tmp/downloads\n");

        // Create fixture: Spring Boot config class with @Data + @Value
        Path javaDir = repoRoot.resolve("src/main/java/com/example");
        Files.createDirectories(javaDir);
        Path configFile = javaDir.resolve("AppConfig.java");
        Files.writeString(configFile,
            "package com.example;\n" +
            "import lombok.Data;\n" +
            "import org.springframework.beans.factory.annotation.Value;\n" +
            "import org.springframework.boot.context.properties.ConfigurationProperties;\n" +
            "import org.springframework.stereotype.Component;\n" +
            "@Data\n" +
            "@Component\n" +
            "public class AppConfig {\n" +
            "    @Value(\"${app.path-download}\")\n" +
            "    private String pathDownload;\n" +
            "}\n"
        );

        // Create fixture: service that calls config.getPathDownload()
        Path serviceFile = javaDir.resolve("DownloadService.java");
        Files.writeString(serviceFile,
            "package com.example;\n" +
            "import org.springframework.stereotype.Service;\n" +
            "@Service\n" +
            "public class DownloadService {\n" +
            "    private final AppConfig config;\n" +
            "    public DownloadService(AppConfig config) {\n" +
            "        this.config = config;\n" +
            "    }\n" +
            "    public void download() {\n" +
            "        String path = config.getPathDownload();\n" +
            "        System.out.println(\"Downloading to: \" + path);\n" +
            "    }\n" +
            "}\n"
        );

        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute("app.path-download", repoRoot, 3);

        // Should find results
        assertFalse(result.iterations().isEmpty(),
            "Should find results for 'app.path-download'");

        // Should find the config key reference
        boolean foundConfigKey = result.iterations().stream()
            .anyMatch(iter -> iter.sourceReference().name().equals("app.path-download"));
        assertTrue(foundConfigKey, "Should find 'app.path-download' as a CONFIG_KEY reference");

        // Should find getPathDownload() usage in DownloadService
        boolean foundGetterUsage = result.iterations().stream()
            .flatMap(iter -> iter.usages().stream())
            .anyMatch(usage -> usage.file().toString().contains("DownloadService") &&
                               usage.astNode().toString().contains("getPathDownload"));
        assertTrue(foundGetterUsage,
            "Should find getPathDownload() usage in DownloadService.java via Lombok @Data derivation");
    }

    /**
     * Test 2: Repository without Lombok produces identical results to pre-Lombok behavior.
     *
     * A plain Spring Boot config class (no Lombok) with an explicit getTimeout() method
     * should be found via the normal search path, not via Lombok derivation.
     * No extra usages should be added by the Lombok path.
     *
     * Validates: Requirements 4.3, 4.4
     */
    @Test
    void repositoryWithoutLombokProducesIdenticalResults() throws IOException {
        // Create fixture: application.properties
        Path resourcesDir = repoRoot.resolve("src/main/resources");
        Files.createDirectories(resourcesDir);
        Path propsFile = resourcesDir.resolve("application.properties");
        Files.writeString(propsFile, "app.timeout=30\n");

        // Create fixture: plain Spring Boot config class (NO Lombok) with explicit getter
        Path javaDir = repoRoot.resolve("src/main/java/com/example");
        Files.createDirectories(javaDir);
        Path configFile = javaDir.resolve("TimeoutConfig.java");
        Files.writeString(configFile,
            "package com.example;\n" +
            "import org.springframework.beans.factory.annotation.Value;\n" +
            "import org.springframework.stereotype.Component;\n" +
            "@Component\n" +
            "public class TimeoutConfig {\n" +
            "    @Value(\"${app.timeout}\")\n" +
            "    private int timeout;\n" +
            "    public int getTimeout() {\n" +
            "        return timeout;\n" +
            "    }\n" +
            "}\n"
        );

        // Create fixture: service that calls config.getTimeout()
        Path serviceFile = javaDir.resolve("TimeoutService.java");
        Files.writeString(serviceFile,
            "package com.example;\n" +
            "import org.springframework.stereotype.Service;\n" +
            "@Service\n" +
            "public class TimeoutService {\n" +
            "    private final TimeoutConfig config;\n" +
            "    public TimeoutService(TimeoutConfig config) {\n" +
            "        this.config = config;\n" +
            "    }\n" +
            "    public void process() {\n" +
            "        int t = config.getTimeout();\n" +
            "        System.out.println(\"Timeout: \" + t);\n" +
            "    }\n" +
            "}\n"
        );

        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute("app.timeout", repoRoot, 3);

        // Should find results
        assertFalse(result.iterations().isEmpty(),
            "Should find results for 'app.timeout'");

        // Should find the @Value annotation usage in TimeoutConfig
        boolean foundValueAnnotation = result.iterations().stream()
            .flatMap(iter -> iter.usages().stream())
            .anyMatch(usage -> usage.file().toString().contains("TimeoutConfig"));
        assertTrue(foundValueAnnotation,
            "Should find @Value annotation usage of 'app.timeout' in TimeoutConfig.java");

        // Should find the explicit getTimeout() call in TimeoutService
        boolean foundExplicitGetter = result.iterations().stream()
            .flatMap(iter -> iter.usages().stream())
            .anyMatch(usage -> usage.file().toString().contains("TimeoutService") &&
                               usage.astNode().toString().contains("getTimeout"));
        assertTrue(foundExplicitGetter,
            "Should find explicit getTimeout() call in TimeoutService.java via normal search");

        // Count total usages of getTimeout across all iterations
        long getTimeoutUsageCount = result.iterations().stream()
            .flatMap(iter -> iter.usages().stream())
            .filter(usage -> usage.astNode().toString().contains("getTimeout"))
            .count();

        // The explicit getter should be found exactly once (via normal search, not duplicated by Lombok path)
        // Since TimeoutConfig has no Lombok annotations, the Lombok path should not add extra usages
        assertEquals(1, getTimeoutUsageCount,
            "getTimeout() should be found exactly once (via normal search, not duplicated by Lombok path)");
    }

    /**
     * Test 3: Single-letter field name (e.g., {@code x} → {@code getX}).
     *
     * A field named {@code x} on a {@code @Data} class should cause the engine to find
     * usages of {@code getX()} in a consumer class.
     *
     * Validates: Requirements 2.1, 4.4
     */
    @Test
    void singleLetterFieldNameDerivesCorrectGetter(@TempDir Path tempDir) throws IOException {
        Path javaDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(javaDir);

        // Source: @Data class with single-letter field x
        Path configFile = javaDir.resolve("Config.java");
        Files.writeString(configFile,
            "package com.example;\n" +
            "import lombok.Data;\n" +
            "@Data\n" +
            "public class Config {\n" +
            "    private String x;\n" +
            "}\n"
        );

        // Consumer: calls c.getX()
        Path consumerFile = javaDir.resolve("Consumer.java");
        Files.writeString(consumerFile,
            "package com.example;\n" +
            "public class Consumer {\n" +
            "    void use(Config c) {\n" +
            "        c.getX();\n" +
            "    }\n" +
            "}\n"
        );

        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute("x", tempDir, 3);

        // Should find getX() usage in Consumer
        boolean foundGetterUsage = result.iterations().stream()
            .flatMap(iter -> iter.usages().stream())
            .anyMatch(usage -> usage.file().toString().contains("Consumer") &&
                               usage.astNode().toString().contains("getX"));
        assertTrue(foundGetterUsage,
            "Should find getX() usage in Consumer.java via Lombok @Data derivation for single-letter field 'x'");
    }

    /**
     * Test 4: Field name starting with uppercase (e.g., {@code URL} → {@code getURL}).
     *
     * A field named {@code URL} on a {@code @Data} class should cause the engine to find
     * usages of {@code getURL()} in a consumer class. Lombok's capitalize keeps the rest
     * of the name unchanged, so {@code URL} → {@code getURL}.
     *
     * Validates: Requirements 2.1, 4.4
     */
    @Test
    void uppercaseFieldNameDerivesCorrectGetter(@TempDir Path tempDir) throws IOException {
        Path javaDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(javaDir);

        // Source: @Data class with uppercase field URL
        Path configFile = javaDir.resolve("Config.java");
        Files.writeString(configFile,
            "package com.example;\n" +
            "import lombok.Data;\n" +
            "@Data\n" +
            "public class Config {\n" +
            "    private String URL;\n" +
            "}\n"
        );

        // Consumer: calls c.getURL()
        Path consumerFile = javaDir.resolve("Consumer.java");
        Files.writeString(consumerFile,
            "package com.example;\n" +
            "public class Consumer {\n" +
            "    void use(Config c) {\n" +
            "        c.getURL();\n" +
            "    }\n" +
            "}\n"
        );

        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute("URL", tempDir, 3);

        // Should find getURL() usage in Consumer
        boolean foundGetterUsage = result.iterations().stream()
            .flatMap(iter -> iter.usages().stream())
            .anyMatch(usage -> usage.file().toString().contains("Consumer") &&
                               usage.astNode().toString().contains("getURL"));
        assertTrue(foundGetterUsage,
            "Should find getURL() usage in Consumer.java via Lombok @Data derivation for uppercase field 'URL'");
    }

    /**
     * Test 5: Boolean field with {@code is} prefix already in name
     * (e.g., {@code isActive} → {@code isIsActive}).
     *
     * Lombok's real behavior: for a boolean field named {@code isActive}, the generated
     * getter is {@code isIsActive()} (prefix {@code is} + capitalize({@code isActive})
     * = {@code isIsActive}).
     *
     * Validates: Requirements 2.2, 4.4
     */
    @Test
    void booleanFieldWithIsPrefixDerivesIsIsGetter(@TempDir Path tempDir) throws IOException {
        Path javaDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(javaDir);

        // Source: @Data class with boolean field isActive
        Path configFile = javaDir.resolve("Config.java");
        Files.writeString(configFile,
            "package com.example;\n" +
            "import lombok.Data;\n" +
            "@Data\n" +
            "public class Config {\n" +
            "    private boolean isActive;\n" +
            "}\n"
        );

        // Consumer: calls c.isIsActive() — Lombok's real behavior
        Path consumerFile = javaDir.resolve("Consumer.java");
        Files.writeString(consumerFile,
            "package com.example;\n" +
            "public class Consumer {\n" +
            "    void use(Config c) {\n" +
            "        c.isIsActive();\n" +
            "    }\n" +
            "}\n"
        );

        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute("isActive", tempDir, 3);

        // Should find isIsActive() usage in Consumer
        boolean foundGetterUsage = result.iterations().stream()
            .flatMap(iter -> iter.usages().stream())
            .anyMatch(usage -> usage.file().toString().contains("Consumer") &&
                               usage.astNode().toString().contains("isIsActive"));
        assertTrue(foundGetterUsage,
            "Should find isIsActive() usage in Consumer.java via Lombok @Data derivation for boolean field 'isActive'");
    }

    /**
     * Test 6: Qualified Lombok annotation ({@code @lombok.Data}) mixed with simple ({@code @Getter}).
     *
     * A class annotated with the fully-qualified {@code @lombok.Data} and a field annotated
     * with the simple {@code @Getter} should still have its getter found in a consumer.
     *
     * Validates: Requirements 1.4, 2.1, 4.4
     */
    @Test
    void qualifiedLombokAnnotationMixedWithSimpleFindsGetter(@TempDir Path tempDir) throws IOException {
        Path javaDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(javaDir);

        // Source: class with @lombok.Data (qualified) on class and @Getter on field
        Path configFile = javaDir.resolve("Config.java");
        Files.writeString(configFile,
            "package com.example;\n" +
            "import lombok.Getter;\n" +
            "@lombok.Data\n" +
            "public class Config {\n" +
            "    @Getter\n" +
            "    private String name;\n" +
            "}\n"
        );

        // Consumer: calls c.getName()
        Path consumerFile = javaDir.resolve("Consumer.java");
        Files.writeString(consumerFile,
            "package com.example;\n" +
            "public class Consumer {\n" +
            "    void use(Config c) {\n" +
            "        c.getName();\n" +
            "    }\n" +
            "}\n"
        );

        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute("name", tempDir, 3);

        // Should find getName() usage in Consumer
        boolean foundGetterUsage = result.iterations().stream()
            .flatMap(iter -> iter.usages().stream())
            .anyMatch(usage -> usage.file().toString().contains("Consumer") &&
                               usage.astNode().toString().contains("getName"));
        assertTrue(foundGetterUsage,
            "Should find getName() usage in Consumer.java when class uses @lombok.Data (qualified) and field uses @Getter (simple)");
    }

    /**
     * Test 7: Source file with syntax error — graceful degradation.
     *
     * When a source file contains invalid Java syntax, the engine must not throw an
     * unhandled exception. The result may be empty, but execution must complete normally.
     *
     * Validates: Requirements 4.4
     */
    @Test
    void syntaxErrorInSourceFileDoesNotThrow(@TempDir Path tempDir) throws IOException {
        Path javaDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(javaDir);

        // Source: invalid Java syntax
        Path brokenFile = javaDir.resolve("Broken.java");
        Files.writeString(brokenFile,
            "package com.example;\n" +
            "this is not valid java syntax @@@ {\n" +
            "    private String field;\n" +
            "}\n"
        );

        // Consumer: a valid file that references the field name
        Path consumerFile = javaDir.resolve("Consumer.java");
        Files.writeString(consumerFile,
            "package com.example;\n" +
            "public class Consumer {\n" +
            "    void use() {\n" +
            "        // references field\n" +
            "        Object field = null;\n" +
            "    }\n" +
            "}\n"
        );

        SearchEngineImpl engine = new SearchEngineImpl();

        // Must not throw — graceful degradation
        SearchResult[] resultHolder = new SearchResult[1];
        assertDoesNotThrow(() -> {
            resultHolder[0] = engine.execute("field", tempDir, 3);
        }, "SearchEngine.execute must not throw even when a source file has a syntax error");

        // Result must be non-null (possibly empty)
        assertNotNull(resultHolder[0],
            "SearchResult must be non-null even when a source file has a syntax error");
    }
}
