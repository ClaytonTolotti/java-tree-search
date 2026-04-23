package com.javatreesearch.tracker;

import com.javatreesearch.model.Reference;
import com.javatreesearch.model.UsageNode;

import java.nio.file.Path;
import java.util.List;

/**
 * Locates all usages of a reference across the repository.
 */
public interface ReferenceTracker {
    /**
     * Finds all usage nodes for the given reference.
     * For CONFIG_KEY references, also searches @Value and @ConfigProperty annotations.
     */
    List<UsageNode> findUsages(Reference reference, List<Path> javaFiles, List<Path> configFiles);
}
