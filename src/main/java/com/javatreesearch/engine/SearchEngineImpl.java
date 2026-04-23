package com.javatreesearch.engine;

import com.javatreesearch.model.*;
import com.javatreesearch.parser.AstParserImpl;
import com.javatreesearch.parser.ConfigParserImpl;
import com.javatreesearch.resolver.ReferenceResolverImpl;
import com.javatreesearch.tracker.ReferenceTrackerImpl;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import java.nio.file.Path;
import java.util.*;

/**
 * Orchestrates the recursive AST search across all iterations.
 *
 * Flow:
 * 1. List all Java and config files in rootDir.
 * 2. Iteration 1: search for the term in all files, extract references.
 * 3. Iterations 2..maxDepth: for each reference found, track usages and extract new references.
 * 4. Avoid cycles via a Set of visited reference names.
 * 5. Show progress bar on stderr when repository has > 1000 files.
 *
 * Req 1.1–1.6, 3.1, 3.4, 4.4–4.7, 6.2
 */
public class SearchEngineImpl implements SearchEngine {

    private static final int PROGRESS_THRESHOLD = 1000;

    private final AstParserImpl astParser;
    private final ConfigParserImpl configParser;
    private final ReferenceResolverImpl resolver;
    private final ReferenceTrackerImpl tracker;

    public SearchEngineImpl() {
        this.astParser = new AstParserImpl();
        this.configParser = new ConfigParserImpl();
        this.resolver = new ReferenceResolverImpl();
        this.tracker = new ReferenceTrackerImpl(astParser);
    }

    @Override
    public SearchResult execute(String term, Path rootDir, int maxDepth) {
        List<Path> javaFiles = astParser.listJavaFiles(rootDir);
        List<Path> configFiles = configParser.listConfigFiles(rootDir);
        int totalFiles = javaFiles.size() + configFiles.size();

        boolean showProgress = totalFiles > PROGRESS_THRESHOLD;
        if (showProgress) {
            System.err.println("[INFO] Scanning " + totalFiles + " files...");
        }

        List<IterationResult> iterations = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        // Iteration 1: find initial references for the search term
        List<Reference> currentRefs = findInitialReferences(term, javaFiles, configFiles);

        if (currentRefs.isEmpty()) {
            System.err.println("[INFO] No results found for term: " + term);
            return new SearchResult(term, rootDir, maxDepth, iterations);
        }

        // Queue: (reference, depth)
        Deque<Map.Entry<Reference, Integer>> queue = new ArrayDeque<>();
        for (Reference ref : currentRefs) {
            if (visited.add(ref.name())) {
                queue.add(Map.entry(ref, 1));
            }
        }

        int processed = 0;
        while (!queue.isEmpty()) {
            Map.Entry<Reference, Integer> entry = queue.poll();
            Reference ref = entry.getKey();
            int depth = entry.getValue();

            if (depth > maxDepth) continue;

            if (showProgress) {
                processed++;
                if (processed % 100 == 0) {
                    System.err.println("[INFO] Processed " + processed + " references...");
                }
            }

            // Track usages of this reference
            List<UsageNode> usages = tracker.findUsages(ref, javaFiles, configFiles);
            iterations.add(new IterationResult(depth, ref, usages));

            // If we haven't reached maxDepth, enqueue new references from usages
            if (depth < maxDepth) {
                for (UsageNode usage : usages) {
                    Optional<Reference> newRef = resolver.resolveFromNode(usage.astNode(), usage.file());
                    newRef.ifPresent(r -> {
                        if (visited.add(r.name())) {
                            queue.add(Map.entry(r, depth + 1));
                        }
                    });
                }
            }
        }

        return new SearchResult(term, rootDir, maxDepth, iterations);
    }

    /**
     * Finds initial references for the search term in all Java and config files.
     * Req 3.1, 3.2, 3.3
     */
    private List<Reference> findInitialReferences(String term, List<Path> javaFiles, List<Path> configFiles) {
        List<Reference> refs = new ArrayList<>();

        // Search in Java files
        for (Path file : javaFiles) {
            Optional<CompilationUnit> cuOpt;
            try {
                cuOpt = astParser.getAst(file, term);
            } catch (AstParserImpl.AstIoException | AstParserImpl.AstSyntaxException e) {
                System.err.println("[WARN] " + e.getMessage());
                continue;
            }
            if (cuOpt.isEmpty()) continue;

            CompilationUnit cu = cuOpt.get();
            // Walk all nodes and find those whose string representation contains the term
            cu.walk(node -> {
                if (nodeContainsTerm(node, term)) {
                    resolver.resolveFromNode(node, file).ifPresent(ref -> {
                        if (!refs.stream().anyMatch(r -> r.name().equals(ref.name()) && r.file().equals(ref.file()))) {
                            refs.add(ref);
                        }
                    });
                }
            });
        }

        // Search in config files
        for (Path file : configFiles) {
            try {
                List<ConfigEntry> entries = configParser.getEntries(file, term);
                for (ConfigEntry entry : entries) {
                    refs.add(resolver.resolveFromConfigEntry(entry));
                }
            } catch (ConfigParserImpl.ConfigIoException e) {
                System.err.println("[WARN] " + e.getMessage());
            }
        }

        return refs;
    }

    private boolean nodeContainsTerm(Node node, String term) {
        // Check if the node's string representation contains the term
        // This is a broad check; the resolver will narrow it down
        return node.toString().contains(term);
    }
}
