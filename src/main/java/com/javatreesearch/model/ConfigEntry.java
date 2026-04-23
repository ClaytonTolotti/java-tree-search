package com.javatreesearch.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A key-value entry extracted from a configuration file.
 * Req 2a.3: must contain key, value, file path, line number.
 */
public record ConfigEntry(
    String key,
    String value,
    Path file,
    int line
) {
    public ConfigEntry {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(file, "file must not be null");
        if (line <= 0) throw new IllegalArgumentException("line must be > 0, got: " + line);
    }
}
