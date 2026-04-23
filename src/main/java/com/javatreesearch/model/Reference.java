package com.javatreesearch.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A resolved reference (definition) found in the codebase.
 * Req 3.2: must contain name, type, file path, line number, column number.
 */
public record Reference(
    String name,
    ReferenceType type,
    Path file,
    int line,
    int column
) {
    public Reference {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(file, "file must not be null");
        if (line <= 0) throw new IllegalArgumentException("line must be > 0, got: " + line);
        if (column < 0) throw new IllegalArgumentException("column must be >= 0, got: " + column);
    }
}
