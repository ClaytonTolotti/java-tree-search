package com.javatreesearch.tracker;

// Feature: lombok-reference-support, Property 10: Rastreamento Lombok para CONFIG_KEY

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
 * Property-based tests for ReferenceTracker CONFIG_KEY + Lombok tracking.
 *
 * Validates: Requirement 3.3
 */
class ReferenceTrackerConfigKeyLombokPropertiesTest {

    /**
     * Property 10: Rastreamento Lombok para CONFIG_KEY
     *
     * For any CONFIG_KEY Reference whose Java field annotated with @Value/@ConfigProperty
     * belongs to a class with LombokAnnotation, the ReferenceTracker SHALL include in
     * findUsages results the MethodCallExprs corresponding to the LombokDerivedMethods
     * of that field.
     *
     * Validates: Requirements 3.3
     */
    @Property(tries = 30)
    void configKeyLombokDerivedMethodsAreTracked(
            @ForAll("validFieldNames") String fieldName
    ) throws IOException {
        // The config key used in @Value annotation
        String configKey = "app.key";

        // Derive the getter name for the field (non-boolean String field → getXxx)
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        // 1. Config file: application.properties with app.key=value
        String configFileContent = "app.key=value\n";

        // 2. Spring Boot config class annotated with @Data, field annotated with @Value("${app.key}")
        String configClassContent = String.format(
            "import lombok.Data;\n" +
            "import org.springframework.beans.factory.annotation.Value;\n" +
            "@Data\n" +
            "public class AppConfig {\n" +
            "    @Value(\"${app.key}\")\n" +
            "    private String %s;\n" +
            "}\n",
            fieldName
        );

        // 3. Consumer class that calls the getter on the config class
        String consumerClassContent = String.format(
            "public class AppConsumer {\n" +
            "    void use(AppConfig config) {\n" +
            "        String val = config.%s();\n" +
            "    }\n" +
            "}\n",
            getterName
        );

        Path tempDir = Files.createTempDirectory("config-key-lombok-test");
        try {
            Path configFile = tempDir.resolve("application.properties");
            Path sourceFile = tempDir.resolve("AppConfig.java");
            Path consumerFile = tempDir.resolve("AppConsumer.java");

            Files.writeString(configFile, configFileContent);
            Files.writeString(sourceFile, configClassContent);
            Files.writeString(consumerFile, consumerClassContent);

            // Reference with type CONFIG_KEY, name "app.key", pointing to the config file
            Reference reference = new Reference(configKey, ReferenceType.CONFIG_KEY, configFile, 1, 0);

            ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(new AstParserImpl());
            List<UsageNode> usages = tracker.findUsages(
                reference,
                List.of(sourceFile, consumerFile),
                List.of(configFile)
            );

            // Assert that the result contains at least one UsageNode whose astNode().toString()
            // contains the getter name
            boolean hasGetterUsage = usages.stream()
                .anyMatch(u -> u.astNode().toString().contains(getterName));

            assertTrue(hasGetterUsage,
                "findUsages must include a UsageNode containing getter '" + getterName +
                "' for CONFIG_KEY '" + configKey + "' with field '" + fieldName +
                "'. Found usages: " + usages.stream()
                    .map(u -> u.astNode().toString())
                    .toList());

        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Provide
    Arbitrary<String> validFieldNames() {
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
