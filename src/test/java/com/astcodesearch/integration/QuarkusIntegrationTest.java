package com.javatreesearch.integration;

import com.javatreesearch.engine.SearchEngineImpl;
import com.javatreesearch.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test with Quarkus fixture.
 * Validates: Requirements 4.2, 4.3
 */
class QuarkusIntegrationTest {

    @TempDir
    Path repoRoot;

    /**
     * Quarkus fixture: application.properties defines a key,
     * a resource uses it via @ConfigProperty(name = "key").
     * Verifies that the config key is tracked via @ConfigProperty annotation.
     */
    @Test
    void quarkusConfigPropertyAnnotationIsTracked() throws IOException {
        // Create fixture: application.properties
        Path resourcesDir = repoRoot.resolve("src/main/resources");
        Files.createDirectories(resourcesDir);
        Path propsFile = resourcesDir.resolve("application.properties");
        Files.writeString(propsFile, "quarkus.http.port=8080\nquarkus.datasource.url=jdbc:h2:mem:test\n");

        // Create fixture: resource using @ConfigProperty
        Path javaDir = repoRoot.resolve("src/main/java/com/example");
        Files.createDirectories(javaDir);
        Path resourceFile = javaDir.resolve("GreetingResource.java");
        Files.writeString(resourceFile,
            "package com.example;\n" +
            "import io.smallrye.config.ConfigProperty;\n" +
            "import jakarta.ws.rs.GET;\n" +
            "import jakarta.ws.rs.Path;\n" +
            "@Path(\"/hello\")\n" +
            "public class GreetingResource {\n" +
            "    @ConfigProperty(name = \"quarkus.http.port\")\n" +
            "    int httpPort;\n" +
            "    @GET\n" +
            "    public String hello() { return \"Port: \" + httpPort; }\n" +
            "}\n"
        );

        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute("quarkus.http.port", repoRoot, 3);

        assertFalse(result.iterations().isEmpty(),
            "Should find results for 'quarkus.http.port'");

        boolean foundConfigKey = result.iterations().stream()
            .anyMatch(iter -> iter.sourceReference().name().equals("quarkus.http.port"));
        assertTrue(foundConfigKey, "Should find 'quarkus.http.port' as a CONFIG_KEY reference");

        boolean foundConfigPropertyUsage = result.iterations().stream()
            .flatMap(iter -> iter.usages().stream())
            .anyMatch(usage -> usage.file().toString().contains("GreetingResource"));
        assertTrue(foundConfigPropertyUsage,
            "Should find @ConfigProperty usage of 'quarkus.http.port' in GreetingResource.java");
    }
}
