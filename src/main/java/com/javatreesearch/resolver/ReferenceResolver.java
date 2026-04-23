package com.javatreesearch.resolver;

import com.javatreesearch.model.ConfigEntry;
import com.javatreesearch.model.Reference;
import com.github.javaparser.ast.Node;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Extracts Reference definitions from AST nodes or config entries.
 */
public interface ReferenceResolver {
    /** Extracts a definition reference from an AST node. */
    Optional<Reference> resolveFromNode(Node node, Path file);

    /** Extracts a CONFIG_KEY reference from a config entry. */
    Reference resolveFromConfigEntry(ConfigEntry entry);
}
