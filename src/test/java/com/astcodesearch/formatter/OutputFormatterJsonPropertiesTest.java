package com.javatreesearch.formatter;

// Feature: ast-code-search, Property 16: Saída JSON é Válida e Completa

import com.javatreesearch.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.expr.NameExpr;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotBlank;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for OutputFormatter JSON output validity and completeness.
 * Validates: Requirement 5.5
 */
class OutputFormatterJsonPropertiesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Property 16: Saída JSON é Válida e Completa
     *
     * For any SearchResult, when output is redirected (stdout not a terminal),
     * the produced JSON must be syntactically valid and contain all model fields:
     * searchTerm, rootDir, maxDepth, and for each iteration: depth, sourceReference
     * (with all sub-fields) and usages (with all sub-fields).
     *
     * Validates: Requirement 5.5
     */
    @Property(tries = 100)
    void jsonOutputIsValidAndComplete(
            @ForAll @NotBlank String searchTerm,
            @ForAll @IntRange(min = 1, max = 5) int maxDepth,
            @ForAll @IntRange(min = 1, max = 3) int iterationCount,
            @ForAll @IntRange(min = 0, max = 2) int usagesPerIteration
    ) throws Exception {
        String sanitizedTerm = searchTerm.replaceAll("[^a-zA-Z0-9]", "X");
        if (sanitizedTerm.isEmpty()) sanitizedTerm = "term";

        Path rootDir = Path.of("/repo");
        List<IterationResult> iterations = new ArrayList<>();

        for (int i = 1; i <= iterationCount; i++) {
            Path file = Path.of("File" + i + ".java");
            Reference ref = new Reference("ref" + i, ReferenceType.METHOD, file, i, 0);
            List<UsageNode> usages = new ArrayList<>();
            for (int u = 0; u < usagesPerIteration; u++) {
                usages.add(new UsageNode(new NameExpr("ref" + i), file, i + u + 1, u));
            }
            iterations.add(new IterationResult(i, ref, usages));
        }

        SearchResult result = new SearchResult(sanitizedTerm, rootDir, maxDepth, iterations);

        // Force JSON output
        OutputFormatterImpl formatter = new OutputFormatterImpl(true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        formatter.render(result, new PrintStream(baos));
        String json = baos.toString().trim();

        // Must be syntactically valid JSON
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (Exception e) {
            fail("Output must be valid JSON, but got parse error: " + e.getMessage() + "\nOutput: " + json);
            return;
        }

        // Must contain all top-level fields
        assertTrue(root.has("searchTerm"), "JSON must have 'searchTerm' field");
        assertTrue(root.has("rootDir"), "JSON must have 'rootDir' field");
        assertTrue(root.has("maxDepth"), "JSON must have 'maxDepth' field");
        assertTrue(root.has("iterations"), "JSON must have 'iterations' field");

        assertEquals(sanitizedTerm, root.get("searchTerm").asText());
        assertEquals(maxDepth, root.get("maxDepth").asInt());

        // Each iteration must have required fields
        JsonNode iters = root.get("iterations");
        assertTrue(iters.isArray(), "'iterations' must be an array");
        assertEquals(iterationCount, iters.size());

        for (JsonNode iter : iters) {
            assertTrue(iter.has("depth"), "Iteration must have 'depth'");
            assertTrue(iter.has("sourceReference"), "Iteration must have 'sourceReference'");
            assertTrue(iter.has("usages"), "Iteration must have 'usages'");

            JsonNode srcRef = iter.get("sourceReference");
            assertTrue(srcRef.has("name"), "sourceReference must have 'name'");
            assertTrue(srcRef.has("type"), "sourceReference must have 'type'");
            assertTrue(srcRef.has("file"), "sourceReference must have 'file'");
            assertTrue(srcRef.has("line"), "sourceReference must have 'line'");
            assertTrue(srcRef.has("column"), "sourceReference must have 'column'");

            JsonNode usages = iter.get("usages");
            assertTrue(usages.isArray(), "'usages' must be an array");
            assertEquals(usagesPerIteration, usages.size());

            for (JsonNode usage : usages) {
                assertTrue(usage.has("file"), "Usage must have 'file'");
                assertTrue(usage.has("line"), "Usage must have 'line'");
                assertTrue(usage.has("column"), "Usage must have 'column'");
            }
        }
    }
}
