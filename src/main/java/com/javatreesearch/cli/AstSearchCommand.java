package com.javatreesearch.cli;

import com.javatreesearch.engine.SearchEngineImpl;
import com.javatreesearch.formatter.OutputFormatterImpl;
import com.javatreesearch.model.SearchResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * picocli command for AST-based code search.
 *
 * Usage: ast-search <searchTerm> [directory] [depth]
 *
 * Req 1.1–1.7
 */
@Command(
    name = "ast-search",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Recursively searches Java repositories using AST analysis."
)
public class AstSearchCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Search term (required)")
    String searchTerm;

    @Parameters(index = "1", defaultValue = ".", description = "Root directory to search (default: current directory)")
    Path directory;

    @Parameters(index = "2", defaultValue = "5", description = "Maximum search depth, must be a positive integer (default: 5)")
    String depthStr;

    @Override
    public Integer call() {
        // Validate depth parameter (Req 1.7)
        int depth;
        try {
            depth = Integer.parseInt(depthStr);
            if (depth <= 0) {
                System.err.println("Error: depth must be a positive integer, got: " + depthStr);
                CommandLine.usage(this, System.err);
                return 1;
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: depth must be a positive integer, got: '" + depthStr + "'");
            CommandLine.usage(this, System.err);
            return 1;
        }

        // Validate directory (Req 1.3)
        Path rootDir = directory.toAbsolutePath().normalize();
        if (!Files.exists(rootDir)) {
            System.err.println("Error: directory does not exist: " + rootDir);
            return 1;
        }
        if (!Files.isDirectory(rootDir)) {
            System.err.println("Error: path is not a directory: " + rootDir);
            return 1;
        }
        if (!Files.isReadable(rootDir)) {
            System.err.println("Error: directory is not accessible: " + rootDir);
            return 1;
        }

        // Execute search
        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute(searchTerm, rootDir, depth);

        // Render output
        OutputFormatterImpl formatter = new OutputFormatterImpl();
        formatter.render(result, System.out);

        return 0;
    }
}
