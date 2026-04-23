package com.javatreesearch.integration;

import com.javatreesearch.engine.SearchEngineImpl;
import com.javatreesearch.formatter.OutputFormatterImpl;
import com.javatreesearch.model.SearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for JSON redirected output.
 * Validates: Requirement 5.5
 */
class JsonOutputIntegrationTest {

    @TempDir
    Path repoRoot;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * When forceJson=true (simulating System.console() == null), JSON format is triggered.
     * Verifies the JSON is syntactically valid and contains all required fields.
     * Req 5.5
     */
    @Test
    void redirectedOutputProducesValidCompleteJson() throws IOException {
        // Create a simple Java fixture
        Path javaDir = repoRoot.resolve("src/main/java");
        Files.createDirectories(javaDir);
        Path javaFile = javaDir.resolve("Calculator.java");
        Files.writeString(javaFile,
            "public class Calculator {\n" +
            "    public int add(int a, int b) { return a + b; }\n" +
            "    public int subtract(int a, int b) { return a - b; }\n" +
            "    void test() { add(1, 2); }\n" +
            "}\n"
        );

        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute("add", repoRoot, 2);

        // Force JSON output (simulates redirected stdout)
        OutputFormatterImpl formatter = new OutputFormatterImpl(true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        formatter.render(result, new PrintStream(baos));
        String json = baos.toString().trim();

        // Must be syntactically valid JSON
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (Exception e) {
            fail("Output must be valid JSON: " + e.getMessage() + "\nOutput: " + json);
            return;
        }

        // Must contain all required top-level fields
        assertTrue(root.has("searchTerm"), "JSON must have 'searchTerm'");
        assertTrue(root.has("rootDir"), "JSON must have 'rootDir'");
        assertTrue(root.has("maxDepth"), "JSON must have 'maxDepth'");
        assertTrue(root.has("iterations"), "JSON must have 'iterations'");

        assertEquals("add", root.get("searchTerm").asText());
        assertEquals(2, root.get("maxDepth").asInt());

        // Each iteration must have required fields
        JsonNode iterations = root.get("iterations");
        assertTrue(iterations.isArray());
        for (JsonNode iter : iterations) {
            assertTrue(iter.has("depth"));
            assertTrue(iter.has("sourceReference"));
            assertTrue(iter.has("usages"));

            JsonNode srcRef = iter.get("sourceReference");
            assertTrue(srcRef.has("name"));
            assertTrue(srcRef.has("type"));
            assertTrue(srcRef.has("file"));
            assertTrue(srcRef.has("line"));
            assertTrue(srcRef.has("column"));
        }
    }
}
