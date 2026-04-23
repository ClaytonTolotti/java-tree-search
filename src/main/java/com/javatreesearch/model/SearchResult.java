package com.javatreesearch.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * The complete result of an AST code search execution.
 */
public record SearchResult(
    String searchTerm,
    Path rootDir,
    int maxDepth,
    List<IterationResult> iterations
) {
    public SearchResult {
        Objects.requireNonNull(searchTerm, "searchTerm must not be null");
        Objects.requireNonNull(rootDir, "rootDir must not be null");
        Objects.requireNonNull(iterations, "iterations must not be null");
        if (maxDepth <= 0) throw new IllegalArgumentException("maxDepth must be > 0, got: " + maxDepth);
    }
}
