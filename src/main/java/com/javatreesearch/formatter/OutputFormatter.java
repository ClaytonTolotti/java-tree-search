package com.javatreesearch.formatter;

import com.javatreesearch.model.SearchResult;

import java.io.PrintStream;

/**
 * Renders search results as hierarchical text or JSON depending on output context.
 */
public interface OutputFormatter {
    void render(SearchResult result, PrintStream out);
}
