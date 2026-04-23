package com.javatreesearch.formatter;

import com.javatreesearch.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.expr.NameExpr;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example-based tests for OutputFormatterImpl.
 * Validates: Requirement 5.5
 */
class OutputFormatterExampleTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * forceJson=true → JSON format is produced.
     * Req 5.5
     */
    @Test
    void forceJsonProducesJsonOutput() throws Exception {
        SearchResult result = buildSampleResult();

        OutputFormatterImpl formatter = new OutputFormatterImpl(true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        formatter.render(result, new PrintStream(baos));
        String output = baos.toString().trim();

        // Must be valid JSON
        JsonNode root = mapper.readTree(output);
        assertTrue(root.has("searchTerm"), "JSON output must have 'searchTerm'");
        assertTrue(root.has("iterations"), "JSON output must have 'iterations'");
        assertEquals("myTerm", root.get("searchTerm").asText());
    }

    /**
     * forceJson=false with no console (simulated by using the hierarchical formatter directly)
     * → hierarchical format is produced.
     * Req 5.5
     */
    @Test
    void hierarchicalOutputContainsDefAndUseMarkers() {
        SearchResult result = buildSampleResult();

        // Use the inner helper from the hierarchical test to force hierarchical output
        OutputFormatterHierarchicalPropertiesTest.HierarchicalOutputFormatter formatter =
            new OutputFormatterHierarchicalPropertiesTest.HierarchicalOutputFormatter();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        formatter.render(result, new PrintStream(baos));
        String output = baos.toString();

        assertTrue(output.contains("[DEF]"), "Hierarchical output must contain [DEF] marker");
        assertTrue(output.contains("[USE]"), "Hierarchical output must contain [USE] marker");
        assertTrue(output.contains("myMethod"), "Hierarchical output must contain reference name");
    }

    private SearchResult buildSampleResult() {
        Path file = Path.of("MyService.java");
        Reference ref = new Reference("myMethod", ReferenceType.METHOD, file, 5, 2);
        UsageNode usage = new UsageNode(new NameExpr("myMethod"), file, 10, 4);
        IterationResult iter = new IterationResult(1, ref, List.of(usage));
        return new SearchResult("myTerm", Path.of("/repo"), 5, List.of(iter));
    }
}
