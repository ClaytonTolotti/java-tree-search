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

/**
 * Integration test with Spring Boot fixture.
 * Validates: Requirements 4.2, 4.3
 */
class SpringBootIntegrationTest {

    @TempDir
    Path repoRoot;

    /**
     * Spring Boot fixture: application.properties defines a key,
     * a service uses it via @Value("${key}").
     * Verifies that the config key is tracked via @Value annotation.
     */
    @Test
    void springBootValueAnnotationIsTracked() throws IOException {
        // Create fixture: application.properties
        Path resourcesDir = repoRoot.resolve("src/main/resources");
        Files.createDirectories(resourcesDir);
        Path propsFile = resourcesDir.resolve("application.properties");
        Files.writeString(propsFile, "app.timeout=30\napp.name=MyApp\n");

        // Create fixture: service using @Value
        Path javaDir = repoRoot.resolve("src/main/java/com/example");
        Files.createDirectories(javaDir);
        Path serviceFile = javaDir.resolve("MyService.java");
        Files.writeString(serviceFile,
            "package com.example;\n" +
            "import org.springframework.beans.factory.annotation.Value;\n" +
            "import org.springframework.stereotype.Service;\n" +
            "@Service\n" +
            "public class MyService {\n" +
            "    @Value(\"${app.timeout}\")\n" +
            "    private int timeout;\n" +
            "    public int getTimeout() { return timeout; }\n" +
            "}\n"
        );

        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute("app.timeout", repoRoot, 3);

        // Should find the config key and its @Value usage
        assertFalse(result.iterations().isEmpty(),
            "Should find results for 'app.timeout'");

        boolean foundConfigKey = result.iterations().stream()
            .anyMatch(iter -> iter.sourceReference().name().equals("app.timeout"));
        assertTrue(foundConfigKey, "Should find 'app.timeout' as a CONFIG_KEY reference");

        // Should find the @Value usage in MyService
        boolean foundValueUsage = result.iterations().stream()
            .flatMap(iter -> iter.usages().stream())
            .anyMatch(usage -> usage.file().toString().contains("MyService"));
        assertTrue(foundValueUsage,
            "Should find @Value usage of 'app.timeout' in MyService.java");
    }
}
