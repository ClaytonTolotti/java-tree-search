package com.javatreesearch.engine;

import com.javatreesearch.model.SearchResult;

import java.nio.file.Path;

/**
 * Orchestrates the recursive AST search across all iterations.
 */
public interface SearchEngine {
    SearchResult execute(String term, Path rootDir, int maxDepth);
}
