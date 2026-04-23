package com.javatreesearch.formatter;

import com.javatreesearch.model.IterationResult;
import com.javatreesearch.model.Reference;
import com.javatreesearch.model.SearchResult;
import com.javatreesearch.model.UsageNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.PrintStream;

/**
 * Renders search results as hierarchical text or JSON.
 *
 * Detection: System.console() == null → JSON output (redirected)
 *            System.console() != null → hierarchical text output (terminal)
 *
 * Req 5.1, 5.2, 5.3, 5.4, 5.5
 */
public class OutputFormatterImpl implements OutputFormatter {

    private final ObjectMapper mapper;
    private final boolean forceJson;

    public OutputFormatterImpl() {
        this(false);
    }

    /** @param forceJson if true, always produce JSON (useful for testing) */
    public OutputFormatterImpl(boolean forceJson) {
        this.forceJson = forceJson;
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void render(SearchResult result, PrintStream out) {
        if (forceJson || System.console() == null) {
            renderJson(result, out);
        } else {
            renderHierarchical(result, out);
        }
    }

    /**
     * Hierarchical text output with indentation by depth level and DEF/USE markers.
     * Req 5.1, 5.2, 5.3, 5.4
     */
    private void renderHierarchical(SearchResult result, PrintStream out) {
        out.println("Search: " + result.searchTerm());
        out.println("Root:   " + result.rootDir());
        out.println("Depth:  " + result.maxDepth());
        out.println();

        if (result.iterations().isEmpty()) {
            out.println("No results found.");
            return;
        }

        for (IterationResult iter : result.iterations()) {
            String indent = "  ".repeat(iter.depth() - 1);
            Reference src = iter.sourceReference();
            out.printf("%s[DEF] %s (%s) @ %s:%d:%d%n",
                indent, src.name(), src.type(), src.file(), src.line(), src.column());

            if (iter.usages().isEmpty()) {
                out.printf("%s  (no usages found)%n", indent);
            } else {
                for (UsageNode usage : iter.usages()) {
                    out.printf("%s  [USE] @ %s:%d:%d%n",
                        indent, usage.file(), usage.line(), usage.column());
                }
            }
        }
    }

    /**
     * JSON output via Jackson Databind with all model fields.
     * Req 5.5
     */
    private void renderJson(SearchResult result, PrintStream out) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("searchTerm", result.searchTerm());
            root.put("rootDir", result.rootDir().toString());
            root.put("maxDepth", result.maxDepth());

            ArrayNode iterations = root.putArray("iterations");
            for (IterationResult iter : result.iterations()) {
                ObjectNode iterNode = iterations.addObject();
                iterNode.put("depth", iter.depth());

                ObjectNode srcNode = iterNode.putObject("sourceReference");
                referenceToJson(iter.sourceReference(), srcNode);

                ArrayNode usagesNode = iterNode.putArray("usages");
                for (UsageNode usage : iter.usages()) {
                    ObjectNode usageNode = usagesNode.addObject();
                    usageNode.put("file", usage.file().toString());
                    usageNode.put("line", usage.line());
                    usageNode.put("column", usage.column());
                }
            }

            out.println(mapper.writeValueAsString(root));
        } catch (Exception e) {
            out.println("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void referenceToJson(Reference ref, ObjectNode node) {
        node.put("name", ref.name());
        node.put("type", ref.type().name());
        node.put("file", ref.file().toString());
        node.put("line", ref.line());
        node.put("column", ref.column());
    }
}
