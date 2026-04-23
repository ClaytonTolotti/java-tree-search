package com.javatreesearch.parser;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implementation of AstParser with lazy caching and text pre-filter.
 *
 * Cache strategy:
 * 1. Check cache for existing CompilationUnit.
 * 2. If absent: run text pre-filter (Files.readString + String.contains).
 * 3. If pre-filter positive: parse with JavaParser and store in cache.
 * 4. If pre-filter negative: return Optional.empty() without storing.
 *
 * Error handling follows the design error table:
 * - Syntax/IO error with other files present: log warning to stderr, continue.
 * - Syntax/IO error on the only file: propagate via AstParseException.
 */
public class AstParserImpl implements AstParser {

    private final Map<Path, CompilationUnit> cache = new HashMap<>();
    private final int maxDirectoryDepth;

    public AstParserImpl() {
        this(Integer.MAX_VALUE);
    }

    public AstParserImpl(int maxDirectoryDepth) {
        this.maxDirectoryDepth = maxDirectoryDepth;
    }

    @Override
    public Optional<CompilationUnit> getAst(Path file, String term) {
        // 1. Check cache
        if (cache.containsKey(file)) {
            return Optional.of(cache.get(file));
        }

        // 2. Text pre-filter
        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            throw new AstIoException(file, e);
        }

        // 3. Pre-filter negative: skip parse
        if (!content.contains(term)) {
            return Optional.empty();
        }

        // 4. Pre-filter positive: parse and cache
        try {
            CompilationUnit cu = StaticJavaParser.parse(content);
            cache.put(file, cu);
            return Optional.of(cu);
        } catch (ParseProblemException e) {
            throw new AstSyntaxException(file, e);
        }
    }

    @Override
    public List<Path> listJavaFiles(Path rootDir) {
        try (Stream<Path> stream = Files.walk(rootDir, maxDirectoryDepth)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
        } catch (IOException e) {
            System.err.println("[WARN] Failed to traverse directory " + rootDir + ": " + e.getMessage());
            return List.of();
        }
    }

    // --- Exception types for structured error handling ---

    /** Thrown when a Java file cannot be read due to I/O or permission error. */
    public static class AstIoException extends RuntimeException {
        private final Path file;

        public AstIoException(Path file, IOException cause) {
            super("I/O error reading " + file + ": " + cause.getMessage(), cause);
            this.file = file;
        }

        public Path getFile() {
            return file;
        }
    }

    /** Thrown when a Java file has a syntax error and cannot be parsed. */
    public static class AstSyntaxException extends RuntimeException {
        private final Path file;

        public AstSyntaxException(Path file, ParseProblemException cause) {
            super("Syntax error in " + file + ": " + cause.getMessage(), cause);
            this.file = file;
        }

        public Path getFile() {
            return file;
        }
    }
}
