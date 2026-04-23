package com.javatreesearch.model;

import com.github.javaparser.ast.Node;

import java.nio.file.Path;
import java.util.Objects;

/**
 * An AST node that uses (references) a previously defined reference.
 */
public record UsageNode(
    Node astNode,
    Path file,
    int line,
    int column
) {
    public UsageNode {
        Objects.requireNonNull(astNode, "astNode must not be null");
        Objects.requireNonNull(file, "file must not be null");
    }
}
