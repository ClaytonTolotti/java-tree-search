package com.javatreesearch.parser;

import com.github.javaparser.ast.CompilationUnit;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Parses Java source files into ASTs with lazy caching.
 */
public interface AstParser {
    /** Returns the AST for the given file, using cache when available. */
    Optional<CompilationUnit> getAst(Path file, String term);

    /** Lists all .java files under rootDir recursively. */
    List<Path> listJavaFiles(Path rootDir);
}
