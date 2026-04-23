package com.javatreesearch.model;

import java.util.List;
import java.util.Objects;

/**
 * Results from a single search iteration (depth level).
 */
public record IterationResult(
    int depth,
    Reference sourceReference,
    List<UsageNode> usages
) {
    public IterationResult {
        Objects.requireNonNull(sourceReference, "sourceReference must not be null");
        Objects.requireNonNull(usages, "usages must not be null");
        if (depth <= 0) throw new IllegalArgumentException("depth must be > 0, got: " + depth);
    }
}
